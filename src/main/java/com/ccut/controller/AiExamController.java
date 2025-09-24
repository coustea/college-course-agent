package com.ccut.controller;

import com.ccut.entity.*;
import com.ccut.mapper.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/aiexam")
public class AiExamController {

    private final ChatClient chatClient;
    private final BeanOutputConverter<Exam> converter;
    private final String format;
    private final CourseMapper courseMapper;
    private final AiExamMapper aiExamMapper;
    private final AiExamQuestionMapper questionMapper;
    private final AiExamAttemptMapper attemptMapper;
    private final AiExamAnswerMapper answerMapper;

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

    public AiExamController(ChatClient.Builder builder,
                            CourseMapper courseMapper,
                            AiExamMapper aiExamMapper,
                            AiExamQuestionMapper questionMapper,
                            AiExamAttemptMapper attemptMapper,
                            AiExamAnswerMapper answerMapper) {
        this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Exam>() {});
        this.format = converter.getFormat();
        this.chatClient = builder.build();
        this.courseMapper = courseMapper;
        this.aiExamMapper = aiExamMapper;
        this.questionMapper = questionMapper;
        this.attemptMapper = attemptMapper;
        this.answerMapper = answerMapper;
    }

    public record GenerateReq(Long courseId, Long studentId, Integer choiceCount, Integer judgeCount) {}
    public record SubmitReq(Long examId, Long studentId, List<SubmitAnswer> answers) {}
    public record SubmitAnswer(Long questionId, String answer) {}

    @PostMapping("/generate")
    public Result<Map<String,Object>> generate(@RequestBody GenerateReq req){
        try {
            if (req == null || req.courseId == null) return Result.error(400, "courseId 不能为空");
            Course course = courseMapper.selectById(req.courseId);
            if (course == null) return Result.error(404, "课程不存在");
            String topic = course.getCourseName();

            int choiceCount = req.choiceCount == null ? 3 : Math.max(0, req.choiceCount);
            int judgeCount = req.judgeCount == null ? 2 : Math.max(0, req.judgeCount);

            PromptTemplate template = new PromptTemplate(PROMPT_TEMPLATE);
            Prompt prompt = template.create(Map.of(
                    "topic", topic,
                    "format", format,
                    "choiceCount", choiceCount,
                    "judgeCount", judgeCount
            ));
            Exam ai = chatClient.prompt(prompt).call().entity(Exam.class);

            AiExam exam = new AiExam();
            exam.setCourseId(req.courseId);
            exam.setCourseName(topic);
            exam.setStudentId(req.studentId);
            exam.setTopic(topic);
            exam.setQuestionCount(ai.getQuestions() == null ? 0 : ai.getQuestions().size());
            exam.setStatus("generated");
            exam.setCreatedAt(new Date());
            aiExamMapper.insert(exam);

            if (ai.getQuestions() != null) {
                for (Question q : ai.getQuestions()) {
                    AiExamQuestion dbq = new AiExamQuestion();
                    dbq.setExamId(exam.getId());
                    dbq.setType(q.getType() == null ? null : q.getType().name());
                    dbq.setContent(q.getQuestion());
                    dbq.setOptions(q.getOptions() == null ? null : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(q.getOptions()));
                    dbq.setAnswer(q.getAnswer());
                    dbq.setAnalysis(q.getAnalysis());
                    questionMapper.insert(dbq);
                }
            }

            Map<String,Object> resp = new HashMap<>();
            resp.put("exam", exam);
            resp.put("questions", questionMapper.listByExamId(exam.getId()));
            return Result.success(resp);
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/submit")
    public Result<Map<String,Object>> submit(@RequestBody SubmitReq req){
        try {
            if (req == null || req.examId == null || req.studentId == null) return Result.error(400, "参数不完整");
            List<AiExamQuestion> qs = questionMapper.listByExamId(req.examId);
            Map<Long, AiExamQuestion> id2q = new HashMap<>();
            for (AiExamQuestion q : qs) id2q.put(q.getId(), q);

            int score = 0; int per = 100 / Math.max(1, qs.size());
            AiExamAttempt attempt = new AiExamAttempt(null, req.examId, req.studentId, 0, null, new Date());
            attemptMapper.insert(attempt);

            if (req.answers != null) {
                for (SubmitAnswer a : req.answers) {
                    AiExamQuestion q = id2q.get(a.questionId);
                    boolean correct = q != null && q.getAnswer() != null && q.getAnswer().trim().equalsIgnoreCase(String.valueOf(a.answer).trim());
                    if (correct) score += per;
                    answerMapper.insert(new AiExamAnswer(null, attempt.getId(), a.questionId, a.answer, correct));
                }
            }

            // 四舍五入处理总分，且不超过100
            score = Math.min(100, Math.max(0, score));
            attempt.setScore(score);
            // 同步写回 attempts 与 exams
            try { attemptMapper.updateScore(attempt.getId(), score); } catch (Exception ignore) {}
            aiExamMapper.updateScoreAndStatus(req.examId, score, "submitted");

            Map<String,Object> resp = new HashMap<>();
            // 直接返回最新的 attempt
            resp.put("attempt", attemptMapper.selectById(attempt.getId()));
            resp.put("answers", req.answers);
            return Result.success(resp);
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}


