package com.ccut.service.Impl;

import com.ccut.dto.CourseEvaluationSummary;
import com.ccut.dto.EvaluationResult;
import com.ccut.entity.*;
import com.ccut.mapper.*;
import com.ccut.service.EvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 教学评价服务实现类
 * 多维度教学评价：内容融入度、师生互动、学生参与度、价值认同
 */
@Slf4j
@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Autowired
    private TeachingEvaluationMapper evaluationMapper;

    @Autowired
    private LearningProgressMapper learningProgressMapper;

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private DocumentProgressMapper documentProgressMapper;

    @Autowired
    private IdeologyResourceMapper ideologyResourceMapper;

    @Autowired
    private IdeologyResourceRecommendationMapper ideologyRecommendationMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Override
    @Transactional
    public EvaluationResult evaluateStudent(Long studentId, Long courseId, String period) {
        log.info("计算学生评价: studentId={}, courseId={}, period={}", studentId, courseId, period);

        // 计算四维度评分
        Double contentScore = calculateContentIntegrationScore(courseId, studentId);
        Double interactionScore = calculateInteractionScore(studentId, courseId);
        Double participationScore = calculateParticipationScore(studentId, courseId);
        Double valueScore = calculateValueRecognitionScore(studentId, courseId);

        // 综合评分（加权平均）
        Double totalScore = 0.25 * contentScore + 0.20 * interactionScore
                          + 0.30 * participationScore + 0.25 * valueScore;

        // 保存评价记录
        TeachingEvaluation evaluation = new TeachingEvaluation();
        evaluation.setCourseId(courseId);
        evaluation.setStudentId(studentId);
        evaluation.setContentIntegrationScore(contentScore);
        evaluation.setInteractionScore(interactionScore);
        evaluation.setParticipationScore(participationScore);
        evaluation.setValueRecognitionScore(valueScore);
        evaluation.setTotalScore(totalScore);
        evaluation.setEvaluationPeriod(period != null ? period : "weekly");
        evaluation.setEvaluationType("student");
        evaluation.setCreatedAt(LocalDateTime.now());

        evaluationMapper.insert(evaluation);

        // 构建返回结果
        return buildEvaluationResult(evaluation, studentId, courseId);
    }

    @Override
    @Transactional
    public List<EvaluationResult> evaluateCourseStudents(Long courseId, String period) {
        log.info("批量计算课程学生评价: courseId={}, period={}", courseId, period);

        List<Long> studentIds = enrollmentMapper.findStudentIdsByCourseId(courseId);
        List<EvaluationResult> results = new ArrayList<>();

        for (Long studentId : studentIds) {
            try {
                EvaluationResult result = evaluateStudent(studentId, courseId, period);
                results.add(result);
            } catch (Exception e) {
                log.error("计算学生评价失败: studentId={}, error={}", studentId, e.getMessage());
            }
        }

        return results;
    }

    @Override
    public List<EvaluationResult> getStudentEvaluationHistory(Long studentId, Long courseId) {
        List<TeachingEvaluation> evaluations = evaluationMapper.selectByStudentId(studentId, courseId);
        return evaluations.stream()
                .map(e -> buildEvaluationResult(e, studentId, courseId))
                .toList();
    }

    @Override
    public CourseEvaluationSummary getCourseSummary(Long courseId, String period) {
        Course course = courseMapper.selectById(courseId);
        List<TeachingEvaluation> evaluations = evaluationMapper.selectByCourseId(courseId, period);

        CourseEvaluationSummary summary = new CourseEvaluationSummary();
        summary.setCourseId(courseId);
        summary.setCourseName(course != null ? course.getCourseName() : "");
        summary.setStudentCount(evaluations.size());

        if (!evaluations.isEmpty()) {
            double avgContent = evaluations.stream().mapToDouble(e -> e.getContentIntegrationScore()).average().orElse(0);
            double avgInteraction = evaluations.stream().mapToDouble(e -> e.getInteractionScore()).average().orElse(0);
            double avgParticipation = evaluations.stream().mapToDouble(e -> e.getParticipationScore()).average().orElse(0);
            double avgValue = evaluations.stream().mapToDouble(e -> e.getValueRecognitionScore()).average().orElse(0);
            double avgTotal = evaluations.stream().mapToDouble(e -> e.getTotalScore()).average().orElse(0);

            summary.setContentIntegrationAvg(avgContent);
            summary.setInteractionAvg(avgInteraction);
            summary.setParticipationAvg(avgParticipation);
            summary.setValueRecognitionAvg(avgValue);
            summary.setOverallScore(avgTotal);
        }

        // 评分分布
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("优秀", 0);
        distribution.put("良好", 0);
        distribution.put("中等", 0);
        distribution.put("待提升", 0);

        for (TeachingEvaluation e : evaluations) {
            String level = getEvaluationLevel(e.getTotalScore());
            distribution.merge(level, 1, Integer::sum);
        }
        summary.setScoreDistribution(distribution);

        // 预警学生
        summary.setWarnings(getWarningStudents(courseId));

        return summary;
    }

    @Override
    public EvaluationResult getEvaluationDetail(Long evaluationId) {
        TeachingEvaluation evaluation = evaluationMapper.selectById(evaluationId);
        if (evaluation == null) {
            return null;
        }
        return buildEvaluationResult(evaluation, evaluation.getStudentId(), evaluation.getCourseId());
    }

    @Override
    public TeachingEvaluation saveEvaluation(TeachingEvaluation evaluation) {
        if (evaluation.getEvaluationId() == null) {
            evaluation.setCreatedAt(LocalDateTime.now());
            evaluationMapper.insert(evaluation);
        } else {
            evaluationMapper.updateById(evaluation);
        }
        return evaluation;
    }

    @Override
    public Double calculateContentIntegrationScore(Long courseId, Long studentId) {
        // 内容融入度：基于思政资源与课程的关联
        // 1. 课程关联的思政资源数量
        List<IdeologyResource> resources = ideologyResourceMapper.search(courseId, null, "published", 100);
        int resourceCount = resources.size();

        // 2. 学生查看的思政资源数量
        int viewedCount = 0;
        if (studentId != null) {
            List<IdeologyResourceRecommendation> recommendations =
                    ideologyRecommendationMapper.findByStudentId(studentId, 100);
            viewedCount = (int) recommendations.stream()
                    .filter(r -> r.getHasClicked() != null && r.getHasClicked())
                    .count();
        }

        // 评分计算：基础分 + 资源覆盖分 + 学生参与分
        double baseScore = 50.0;  // 基础分
        double resourceScore = Math.min(resourceCount * 2, 20);  // 资源数量加分（最多20分）
        double viewScore = resourceCount > 0 ? (viewedCount * 30.0 / resourceCount) : 0;  // 查看比例加分

        return Math.min(baseScore + resourceScore + viewScore, 100);
    }

    @Override
    public Double calculateInteractionScore(Long studentId, Long courseId) {
        // 师生互动：基于AI对话次数、作业提交次数等
        double baseScore = 40.0;

        try {
            // AI对话次数
            List<Conversation> conversations = conversationMapper.findByStudentId(studentId);
            int chatCount = conversations != null ? conversations.size() : 0;
            double chatScore = Math.min(chatCount * 3, 30);  // 每次对话3分，最多30分

            // TODO: 可扩展：作业提交次数、讨论区发言次数等

            return Math.min(baseScore + chatScore, 100);
        } catch (Exception e) {
            log.warn("计算互动评分失败: {}", e.getMessage());
            return baseScore;
        }
    }

    @Override
    public Double calculateParticipationScore(Long studentId, Long courseId) {
        // 学生参与度：基于学习进度、视频完成率等
        LearningProgress progress = learningProgressMapper.findOne(studentId, courseId);
        if (progress == null) {
            return 30.0;  // 无进度记录，返回基础分
        }

        double score = 30.0;  // 基础分

        // 完成百分比加分
        Double completion = progress.getCompletionPercentage();
        if (completion != null) {
            score += completion * 0.5;  // 完成度最多加50分
        }

        // 学习时长加分
        Integer timeSpent = progress.getTimeSpent();
        if (timeSpent != null) {
            // 每小时加2分，最多加20分
            double hours = timeSpent / 3600.0;
            score += Math.min(hours * 2, 20);
        }

        return Math.min(score, 100);
    }

    @Override
    public Double calculateValueRecognitionScore(Long studentId, Long courseId) {
        // 价值认同：基于思政资源点击、学习反思等
        double baseScore = 40.0;

        try {
            // 思政资源推荐点击情况
            List<IdeologyResourceRecommendation> recommendations =
                    ideologyRecommendationMapper.findByStudentId(studentId, 50);

            if (recommendations != null && !recommendations.isEmpty()) {
                int clicked = (int) recommendations.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getHasClicked()))
                        .count();
                double clickRate = (double) clicked / recommendations.size();
                baseScore += clickRate * 40;  // 点击率最多加40分
            }

            // 错题订正情况（反映学习反思）
            // TODO: 可扩展错题相关计算

            return Math.min(baseScore, 100);
        } catch (Exception e) {
            log.warn("计算价值认同评分失败: {}", e.getMessage());
            return baseScore;
        }
    }

    @Override
    public List<CourseEvaluationSummary.StudentWarning> getWarningStudents(Long courseId) {
        List<CourseEvaluationSummary.StudentWarning> warnings = new ArrayList<>();

        // 查找低评分学生
        List<TeachingEvaluation> evaluations = evaluationMapper.selectByCourseId(courseId, "weekly");
        for (TeachingEvaluation e : evaluations) {
            if (e.getTotalScore() < 60) {
                Student student = studentMapper.selectById(e.getStudentId());
                CourseEvaluationSummary.StudentWarning warning = new CourseEvaluationSummary.StudentWarning();
                warning.setStudentId(e.getStudentId());
                warning.setStudentName(student != null ? student.getName() : "未知");
                warning.setWarningType("低评分预警");
                warning.setCurrentScore(e.getTotalScore());
                warning.setSuggestion("建议教师关注该学生学习情况");
                warnings.add(warning);
            }
        }

        return warnings;
    }

    private EvaluationResult buildEvaluationResult(TeachingEvaluation evaluation, Long studentId, Long courseId) {
        EvaluationResult result = new EvaluationResult();
        result.setEvaluationId(evaluation.getEvaluationId());
        result.setCourseId(courseId);
        result.setStudentId(studentId);
        result.setContentIntegrationScore(evaluation.getContentIntegrationScore());
        result.setInteractionScore(evaluation.getInteractionScore());
        result.setParticipationScore(evaluation.getParticipationScore());
        result.setValueRecognitionScore(evaluation.getValueRecognitionScore());
        result.setTotalScore(evaluation.getTotalScore());
        result.setEvaluationLevel(getEvaluationLevel(evaluation.getTotalScore()));

        // 设置课程名称
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            result.setCourseName(course.getCourseName());
        }

        // 设置学生名称
        Student student = studentMapper.selectById(studentId);
        if (student != null) {
            result.setStudentName(student.getName());
        }

        // 生成分析说明和改进建议
        generateAnalysisAndSuggestions(result);

        return result;
    }

    private String getEvaluationLevel(Double score) {
        if (score >= 85) return "优秀";
        if (score >= 70) return "良好";
        if (score >= 60) return "中等";
        return "待提升";
    }

    private void generateAnalysisAndSuggestions(EvaluationResult result) {
        List<String> suggestions = new ArrayList<>();

        // 内容融入分析
        if (result.getContentIntegrationScore() < 60) {
            result.setContentAnalysis("思政资源与课程融合度较低，建议增加课程思政案例。");
            suggestions.add("增加课程思政案例和教学资源");
        } else {
            result.setContentAnalysis("思政资源与课程融合良好。");
        }

        // 互动分析
        if (result.getInteractionScore() < 60) {
            result.setInteractionAnalysis("师生互动较少，建议增加课堂讨论和答疑环节。");
            suggestions.add("积极参与课堂互动和AI辅助学习");
        } else {
            result.setInteractionAnalysis("师生互动情况良好。");
        }

        // 参与度分析
        if (result.getParticipationScore() < 60) {
            result.setParticipationAnalysis("学习参与度较低，建议增加学习时长和完成更多课程内容。");
            suggestions.add("提高学习频率，按时完成课程任务");
        } else {
            result.setParticipationAnalysis("学习参与度良好。");
        }

        // 价值认同分析
        if (result.getValueRecognitionScore() < 60) {
            result.setValueAnalysis("思政学习参与度较低，建议多关注思政资源。");
            suggestions.add("主动学习思政资源，提升思想认识");
        } else {
            result.setValueAnalysis("思政学习情况良好。");
        }

        result.setImprovementSuggestions(suggestions);
    }
}