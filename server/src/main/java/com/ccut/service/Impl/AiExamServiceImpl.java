package com.ccut.service.Impl;

import com.ccut.dto.AiExamAccuracyRequest;
import com.ccut.dto.AiExamGenerateRequest;
import com.ccut.dto.AiExamSubmitAnswer;
import com.ccut.dto.AiExamSubmitRequest;
import com.ccut.dto.Question;
import com.ccut.entity.AiExam;
import com.ccut.entity.Exam;
import com.ccut.entity.AiExamAnswer;
import com.ccut.entity.AiExamAttempt;
import com.ccut.entity.AiExamQuestion;
import com.ccut.entity.Course;
import com.ccut.mapper.AiExamAnswerMapper;
import com.ccut.mapper.AiExamAttemptMapper;
import com.ccut.mapper.AiExamMapper;
import com.ccut.mapper.AiExamQuestionMapper;
import com.ccut.mapper.CourseMapper;
import com.ccut.service.AiExamService;
import com.ccut.service.WrongQuestionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * AI考试服务实现类
 */
@Service
public class AiExamServiceImpl implements AiExamService {

    private final ChatClient chatClient;
    private final BeanOutputConverter<Exam> converter;
    private final String format;
    private final CourseMapper courseMapper;
    private final AiExamMapper aiExamMapper;
    private final AiExamQuestionMapper questionMapper;
    private final AiExamAttemptMapper attemptMapper;
    private final AiExamAnswerMapper answerMapper;
    private final WrongQuestionService wrongQuestionService;

    private static final String PROMPT_TEMPLATE = """
            你是一位经验丰富的出题专家。
            请根据课程名称 "{topic}" 为我生成一份试卷。
            试卷要求如下:
            1. 包含 {choiceCount} 道选择题 (CHOICE)。
            2. 包含 {judgeCount} 道判断题 (JUDGE)。
            对于选择题，必须提供4个选项，'answer' 字段为 A/B/C/D；
            对于判断题，'options' 字段为 null，'answer' 字段为 true/false；
            输出严格遵循以下 JSON 格式：
            {format}
            """;

    public AiExamServiceImpl(ChatClient.Builder builder,
                            CourseMapper courseMapper,
                            AiExamMapper aiExamMapper,
                            AiExamQuestionMapper questionMapper,
                            AiExamAttemptMapper attemptMapper,
                            AiExamAnswerMapper answerMapper,
                            WrongQuestionService wrongQuestionService) {
        this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Exam>() {});
        this.format = converter.getFormat();
        this.chatClient = builder.build();
        this.courseMapper = courseMapper;
        this.aiExamMapper = aiExamMapper;
        this.questionMapper = questionMapper;
        this.attemptMapper = attemptMapper;
        this.answerMapper = answerMapper;
        this.wrongQuestionService = wrongQuestionService;
    }

    @Override
    @Transactional
    public Map<String, Object> generateExam(AiExamGenerateRequest req) {
        if (req == null || req.courseId() == null) {
            throw new IllegalArgumentException("courseId 不能为空");
        }

        Course course = courseMapper.selectById(req.courseId());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        String topic = course.getCourseName();

        int choiceCount = req.choiceCount() == null ? 3 : Math.max(0, req.choiceCount());
        int judgeCount = req.judgeCount() == null ? 2 : Math.max(0, req.judgeCount());

        PromptTemplate template = new PromptTemplate(PROMPT_TEMPLATE);
        Prompt prompt = template.create(Map.of(
                "topic", topic,
                "format", format,
                "choiceCount", choiceCount,
                "judgeCount", judgeCount
        ));
        Exam ai = chatClient.prompt(prompt).call().entity(Exam.class);

        AiExam exam = new AiExam();
        exam.setCourseId(req.courseId());
        exam.setCourseName(topic);
        exam.setStudentId(req.studentId());
        exam.setTopic(topic);
        exam.setQuestionCount(ai.getQuestions() == null ? 0 : ai.getQuestions().size());
        exam.setStatus("generated");
        exam.setCreatedAt(new Date());
        exam.setVideoId(req.videoId());
        exam.setDocumentId(req.documentId());
        aiExamMapper.insert(exam);

        if (ai.getQuestions() != null) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (Question q : ai.getQuestions()) {
                AiExamQuestion dbq = new AiExamQuestion();
                dbq.setExamId(exam.getId());
                dbq.setType(q.type() == null ? null : q.type().name());
                dbq.setContent(q.question());
                try {
                    dbq.setOptions(q.options() == null ? null : mapper.writeValueAsString(q.options()));
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new RuntimeException("选项序列化失败", e);
                }
                dbq.setAnswer(q.answer());
                dbq.setAnalysis(q.analysis());
                questionMapper.insert(dbq);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("exam", exam);
        resp.put("questions", questionMapper.listByExamId(exam.getId()));
        return resp;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(AiExamSubmitRequest req) {
        if (req == null || req.examId() == null || req.studentId() == null) {
            throw new IllegalArgumentException("参数不完整");
        }

        // 查询考试信息以获取课程ID
        AiExam exam = aiExamMapper.selectById(req.examId());
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        Long courseId = exam.getCourseId();

        List<AiExamQuestion> qs = questionMapper.listByExamId(req.examId());
        Map<Long, AiExamQuestion> id2q = new HashMap<>();
        for (AiExamQuestion q : qs) {
            id2q.put(q.getId(), q);
        }

        int score = 0;
        int per = 100 / Math.max(1, qs.size());
        AiExamAttempt attempt = new AiExamAttempt(null, req.examId(), req.studentId(), 0, null, new Date());
        attemptMapper.insert(attempt);

        if (req.answers() != null) {
            for (AiExamSubmitAnswer a : req.answers()) {
                AiExamQuestion q = id2q.get(a.questionId());
                boolean correct = q != null && q.getAnswer() != null && q.getAnswer().trim().equalsIgnoreCase(String.valueOf(a.answer()).trim());
                if (correct) {
                    score += per;
                } else {
                    // 答错了，自动添加到错题本
                    try {
                        wrongQuestionService.addToWrongBook(
                            req.studentId(),
                            a.questionId(),
                            req.examId(),
                            courseId,
                            String.valueOf(a.answer()),
                            q != null ? q.getAnswer() : null
                        );
                    } catch (Exception e) {
                        // 错题本添加失败不影响答题流程
                        // 记录日志即可
                        System.err.println("添加错题失败: " + e.getMessage());
                    }
                }
                answerMapper.insert(new AiExamAnswer(null, attempt.getId(), a.questionId(), a.answer(), correct));
            }
        }

        // 四舍五入处理总分，且不超过100
        score = Math.min(100, Math.max(0, score));
        attempt.setScore(score);
        // 同步写回 attempts 与 exams
        try {
            attemptMapper.updateScore(attempt.getId(), score);
        } catch (Exception ignore) {}
        aiExamMapper.updateScoreAndStatus(req.examId(), score, "submitted");

        Map<String, Object> resp = new HashMap<>();
        // 直接返回最新的 attempt
        resp.put("attempt", attemptMapper.selectById(attempt.getId()));
        resp.put("answers", req.answers());
        return resp;
    }

    @Override
    public Map<String, Object> getAccuracy(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("studentId/courseId 不能为空");
        }

        Double acc = attemptMapper.calcAccuracyByStudentAndCourse(studentId, courseId);
        if (acc == null) {
            acc = 0.0;
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("studentId", studentId);
        resp.put("courseId", courseId);
        resp.put("accuracy", acc);
        resp.put("percentage", Math.round(acc * 10000.0) / 100.0);
        return resp;
    }

    @Override
    public Map<String, Object> getAverageScore(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("studentId 和 courseId 不能为空");
        }

        Double avgScore = aiExamMapper.getAverageScoreByStudentAndCourse(studentId, courseId);
        if (avgScore == null) {
            avgScore = 0.0;
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("studentId", studentId);
        resp.put("courseId", courseId);
        resp.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
        return resp;
    }

    @Override
    public Map<String, Object> getDetailedScores(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("studentId 和 courseId 不能为空");
        }

        // 获取每个视频的成绩
        List<Map<String, Object>> videoScores = aiExamMapper.getVideoScoresByStudentAndCourse(studentId, courseId);
        // 获取每个文档的成绩
        List<Map<String, Object>> documentScores = aiExamMapper.getDocumentScoresByStudentAndCourse(studentId, courseId);

        // 格式化成绩数据，保留两位小数
        for (Map<String, Object> score : videoScores) {
            if (score.get("averageScore") != null) {
                Double avg = ((Number) score.get("averageScore")).doubleValue();
                score.put("averageScore", Math.round(avg * 100.0) / 100.0);
            }
        }
        for (Map<String, Object> score : documentScores) {
            if (score.get("averageScore") != null) {
                Double avg = ((Number) score.get("averageScore")).doubleValue();
                score.put("averageScore", Math.round(avg * 100.0) / 100.0);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("studentId", studentId);
        resp.put("courseId", courseId);
        resp.put("videoScores", videoScores);
        resp.put("documentScores", documentScores);

        return resp;
    }

    @Override
    public List<AiExam> listExams(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("studentId 和 courseId 不能为空");
        }

        return aiExamMapper.listByStudentAndCourse(studentId, courseId);
    }

}
