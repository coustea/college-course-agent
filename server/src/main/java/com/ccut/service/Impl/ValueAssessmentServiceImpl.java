package com.ccut.service.Impl;

import com.ccut.dto.ValueAssessmentSummary;
import com.ccut.dto.ValueTrendPrediction;
import com.ccut.entity.*;
import com.ccut.mapper.*;
import com.ccut.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 价值认同评估服务实现类
 */
@Slf4j
@Service
public class ValueAssessmentServiceImpl implements ValueAssessmentService {

    @Autowired
    private ValueAssessmentMapper valueAssessmentMapper;

    @Autowired
    private TeachingStyleProfileMapper teachingStyleMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private IdeologyResourceMapper ideologyResourceMapper;

    @Autowired
    private IdeologyResourceRecommendationMapper ideologyRecommendationMapper;

    @Autowired
    private LearningProgressMapper learningProgressMapper;

    @Autowired
    private EvaluationService evaluationService;

    @Override
    @Transactional
    public ValueAssessment assessStudentValue(Long studentId, Long courseId, String period) {
        log.info("评估学生价值认同: studentId={}, courseId={}", studentId, courseId);

        // 获取学生思政相关数据
        List<IdeologyResourceRecommendation> recommendations =
                ideologyRecommendationMapper.findByStudentId(studentId, 100);

        // 计算各维度得分
        double patriotismScore = calculatePatriotismScore(studentId, courseId, recommendations);
        double socialResponsibilityScore = calculateSocialResponsibilityScore(studentId, courseId, recommendations);
        double professionalEthicsScore = calculateProfessionalEthicsScore(studentId, courseId);
        double innovationScore = calculateInnovationScore(studentId, courseId);
        double culturalConfidenceScore = calculateCulturalConfidenceScore(studentId, courseId, recommendations);

        // 综合得分
        double totalScore = (patriotismScore * 0.25 + socialResponsibilityScore * 0.2 +
                            professionalEthicsScore * 0.2 + innovationScore * 0.2 +
                            culturalConfidenceScore * 0.15);

        // 获取上期得分
        ValueAssessment lastAssessment = valueAssessmentMapper.findLatestByStudentAndCourse(studentId, courseId);
        Double previousScore = lastAssessment != null ? lastAssessment.getTotalScore() : null;

        // 创建评估记录
        ValueAssessment assessment = new ValueAssessment();
        assessment.setStudentId(studentId);
        assessment.setCourseId(courseId);
        assessment.setPatriotismScore(patriotismScore);
        assessment.setSocialResponsibilityScore(socialResponsibilityScore);
        assessment.setProfessionalEthicsScore(professionalEthicsScore);
        assessment.setInnovationScore(innovationScore);
        assessment.setCulturalConfidenceScore(culturalConfidenceScore);
        assessment.setTotalScore(totalScore);
        assessment.setAssessmentLevel(getAssessmentLevel(totalScore));
        assessment.setPreviousScore(previousScore);
        assessment.setTrendDirection(calculateTrend(totalScore, previousScore));
        assessment.setTrendValue(previousScore != null ? totalScore - previousScore : 0.0);
        assessment.setAssessmentPeriod(period != null ? period : "monthly");
        assessment.setAssessedAt(LocalDateTime.now());
        assessment.setCreatedAt(LocalDateTime.now());

        // 生成评估依据
        assessment.setAssessmentBasis(generateAssessmentBasis(assessment, recommendations));

        valueAssessmentMapper.insert(assessment);
        return assessment;
    }

    @Override
    public List<ValueAssessment> getValueTrend(Long studentId, Long courseId, int months) {
        LocalDateTime startTime = LocalDateTime.now().minusMonths(months);
        return valueAssessmentMapper.findByStudentAndCourseAfter(studentId, courseId, startTime);
    }

    @Override
    @Transactional
    public List<ValueAssessment> batchAssessCourseStudents(Long courseId, String period) {
        List<Long> studentIds = enrollmentMapper.findStudentIdsByCourseId(courseId);
        List<ValueAssessment> results = new ArrayList<>();

        for (Long studentId : studentIds) {
            try {
                results.add(assessStudentValue(studentId, courseId, period));
            } catch (Exception e) {
                log.error("评估学生价值认同失败: studentId={}, error={}", studentId, e.getMessage());
            }
        }

        return results;
    }

    @Override
    public TeachingStyleProfile getTeacherStyleProfile(Long teacherId) {
        return teachingStyleMapper.findByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public TeachingStyleProfile analyzeTeachingStyle(Long teacherId) {
        log.info("分析教师授课风格: teacherId={}", teacherId);

        TeachingStyleProfile profile = teachingStyleMapper.findByTeacherId(teacherId);
        if (profile == null) {
            profile = new TeachingStyleProfile();
            profile.setTeacherId(teacherId);
            profile.setCreatedAt(LocalDateTime.now());
        }

        // 计算各维度得分
        profile.setInteractionLevel(calculateInteractionLevel(teacherId));
        profile.setContentDepth(calculateContentDepth(teacherId));
        profile.setPracticality(calculatePracticality(teacherId));
        profile.setInnovation(calculateInnovation(teacherId));
        profile.setIdeologyIntegration(calculateIdeologyIntegration(teacherId));

        // 思政教学特征
        profile.setIdeologyApproach(determineIdeologyApproach(profile));
        profile.setMainValueThemes(determineMainValueThemes(teacherId));
        profile.setIdeologyResourceCount(countIdeologyResources(teacherId));

        // 风格标签
        profile.setStyleTag(determineStyleTag(profile));

        profile.setLastAnalyzedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        teachingStyleMapper.upsert(profile);
        return profile;
    }

    @Override
    public ValueAssessmentSummary getCourseValueSummary(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        List<ValueAssessment> assessments = valueAssessmentMapper.findByCourseId(courseId);

        ValueAssessmentSummary summary = new ValueAssessmentSummary();
        summary.setCourseId(courseId);
        summary.setCourseName(course != null ? course.getCourseName() : "");
        summary.setStudentCount(assessments.size());

        if (!assessments.isEmpty()) {
            summary.setAvgTotalScore(assessments.stream().mapToDouble(ValueAssessment::getTotalScore).average().orElse(0));
            summary.setAvgPatriotismScore(assessments.stream().mapToDouble(ValueAssessment::getPatriotismScore).average().orElse(0));
            summary.setAvgSocialResponsibilityScore(assessments.stream().mapToDouble(ValueAssessment::getSocialResponsibilityScore).average().orElse(0));
            summary.setAvgProfessionalEthicsScore(assessments.stream().mapToDouble(ValueAssessment::getProfessionalEthicsScore).average().orElse(0));
            summary.setAvgInnovationScore(assessments.stream().mapToDouble(ValueAssessment::getInnovationScore).average().orElse(0));
            summary.setAvgCulturalConfidenceScore(assessments.stream().mapToDouble(ValueAssessment::getCulturalConfidenceScore).average().orElse(0));

            // 分布统计
            List<ValueAssessmentSummary.ValueDistribution> distributions = new ArrayList<>();
            Map<String, Long> levelCount = new HashMap<>();
            for (ValueAssessment a : assessments) {
                levelCount.merge(a.getAssessmentLevel(), 1L, Long::sum);
            }

            for (Map.Entry<String, Long> entry : levelCount.entrySet()) {
                ValueAssessmentSummary.ValueDistribution dist = new ValueAssessmentSummary.ValueDistribution();
                dist.setLevel(entry.getKey());
                dist.setCount(entry.getValue().intValue());
                dist.setPercentage((double) entry.getValue() / assessments.size() * 100);
                distributions.add(dist);
            }
            summary.setDistributions(distributions);
        }

        return summary;
    }

    @Override
    public ValueTrendPrediction predictValueTrend(Long studentId, Long courseId) {
        List<ValueAssessment> history = getValueTrend(studentId, courseId, 6);

        ValueTrendPrediction prediction = new ValueTrendPrediction();
        prediction.setStudentId(studentId);

        if (history.size() < 2) {
            prediction.setPredictedTrend("stable");
            prediction.setPredictedScore(history.isEmpty() ? 60.0 : history.get(0).getTotalScore());
            prediction.setConfidence(0.3);
            prediction.setRecommendation("需要更多数据来进行准确预测");
            return prediction;
        }

        // 简单线性趋势预测
        double[] scores = history.stream().mapToDouble(ValueAssessment::getTotalScore).toArray();
        double trend = scores[scores.length - 1] - scores[0];

        prediction.setPredictedTrend(trend > 5 ? "up" : (trend < -5 ? "down" : "stable"));
        prediction.setPredictedScore(Math.max(0, Math.min(100, scores[scores.length - 1] + trend * 0.5)));
        prediction.setConfidence(Math.min(0.8, 0.5 + history.size() * 0.05));

        // 生成建议
        if ("up".equals(prediction.getPredictedTrend())) {
            prediction.setRecommendation("学生价值认同持续提升，建议继续保持学习热情");
        } else if ("down".equals(prediction.getPredictedTrend())) {
            prediction.setRecommendation("学生价值认同有所下降，建议加强思政引导和互动");
        } else {
            prediction.setRecommendation("学生价值认同稳定，建议通过多样化资源进一步提升");
        }

        // 历史趋势
        List<ValueTrendPrediction.TrendPoint> historicalTrend = new ArrayList<>();
        for (ValueAssessment a : history) {
            ValueTrendPrediction.TrendPoint point = new ValueTrendPrediction.TrendPoint();
            point.setPeriod(a.getAssessmentPeriod());
            point.setScore(a.getTotalScore());
            historicalTrend.add(point);
        }
        prediction.setHistoricalTrend(historicalTrend);

        return prediction;
    }

    // === 私有计算方法 ===

    private double calculatePatriotismScore(Long studentId, Long courseId,
                                             List<IdeologyResourceRecommendation> recommendations) {
        double base = 50.0;
        // 基于家国情怀相关资源的点击和学习
        long patrioticClicks = recommendations.stream()
                .filter(r -> r.getResource() != null && r.getResource().getValueTheme() != null)
                .filter(r -> r.getResource().getValueTheme().contains("家国") ||
                             r.getResource().getValueTheme().contains("爱国"))
                .filter(r -> Boolean.TRUE.equals(r.getHasClicked()))
                .count();
        return Math.min(base + patrioticClicks * 5, 100);
    }

    private double calculateSocialResponsibilityScore(Long studentId, Long courseId,
                                                       List<IdeologyResourceRecommendation> recommendations) {
        double base = 50.0;
        long responsibilityClicks = recommendations.stream()
                .filter(r -> r.getResource() != null && r.getResource().getValueTheme() != null)
                .filter(r -> r.getResource().getValueTheme().contains("责任") ||
                             r.getResource().getValueTheme().contains("社会"))
                .filter(r -> Boolean.TRUE.equals(r.getHasClicked()))
                .count();
        return Math.min(base + responsibilityClicks * 5, 100);
    }

    private double calculateProfessionalEthicsScore(Long studentId, Long courseId) {
        // 基于学习进度和完成率
        LearningProgress progress = learningProgressMapper.findOne(studentId, courseId);
        if (progress == null) return 50.0;
        double completion = progress.getCompletionPercentage() != null ? progress.getCompletionPercentage() : 0;
        return 40 + completion * 0.6;
    }

    private double calculateInnovationScore(Long studentId, Long courseId) {
        // 基于AI互动和错题订正
        // TODO: 可扩展更多指标
        return 60.0;
    }

    private double calculateCulturalConfidenceScore(Long studentId, Long courseId,
                                                     List<IdeologyResourceRecommendation> recommendations) {
        double base = 50.0;
        long cultureClicks = recommendations.stream()
                .filter(r -> r.getResource() != null && r.getResource().getValueTheme() != null)
                .filter(r -> r.getResource().getValueTheme().contains("文化") ||
                             r.getResource().getValueTheme().contains("自信"))
                .filter(r -> Boolean.TRUE.equals(r.getHasClicked()))
                .count();
        return Math.min(base + cultureClicks * 5, 100);
    }

    private String getAssessmentLevel(Double score) {
        if (score >= 80) return "high";
        if (score >= 60) return "medium";
        return "low";
    }

    private String calculateTrend(Double currentScore, Double previousScore) {
        if (previousScore == null) return "stable";
        double diff = currentScore - previousScore;
        if (diff > 5) return "up";
        if (diff < -5) return "down";
        return "stable";
    }

    private String generateAssessmentBasis(ValueAssessment assessment,
                                            List<IdeologyResourceRecommendation> recommendations) {
        StringBuilder basis = new StringBuilder();
        basis.append("基于学生对思政资源的学习情况分析。");

        int clicked = (int) recommendations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getHasClicked()))
                .count();
        basis.append("思政资源参与率：").append(recommendations.isEmpty() ? 0 :
                Math.round(clicked * 100.0 / recommendations.size())).append("%。");

        if (assessment.getTotalScore() >= 80) {
            basis.append("学生展现出较高的价值认同水平。");
        } else if (assessment.getTotalScore() >= 60) {
            basis.append("学生价值认同处于中等水平，有提升空间。");
        } else {
            basis.append("建议加强思政教育引导。");
        }

        return basis.toString();
    }

    private Double calculateInteractionLevel(Long teacherId) {
        // TODO: 基于实际数据计算
        return 70.0;
    }

    private Double calculateContentDepth(Long teacherId) {
        return 65.0;
    }

    private Double calculatePracticality(Long teacherId) {
        return 60.0;
    }

    private Double calculateInnovation(Long teacherId) {
        return 55.0;
    }

    private Double calculateIdeologyIntegration(Long teacherId) {
        int resourceCount = countIdeologyResources(teacherId);
        return (double) Math.min(40 + resourceCount * 3, 100);
    }

    private String determineIdeologyApproach(TeachingStyleProfile profile) {
        if (profile.getIdeologyIntegration() >= 70) return "explicit";
        if (profile.getIdeologyIntegration() >= 50) return "mixed";
        return "implicit";
    }

    private String determineMainValueThemes(Long teacherId) {
        // 查询教师课程关联的思政资源主题
        List<IdeologyResource> resources = ideologyResourceMapper.findByCreatorId(teacherId, 20);
        Set<String> themes = new HashSet<>();
        for (IdeologyResource r : resources) {
            if (r.getValueTheme() != null) {
                themes.add(r.getValueTheme());
            }
        }
        return String.join(",", themes);
    }

    private Integer countIdeologyResources(Long teacherId) {
        List<IdeologyResource> resources = ideologyResourceMapper.findByCreatorId(teacherId, 1000);
        return resources != null ? resources.size() : 0;
    }

    private String determineStyleTag(TeachingStyleProfile profile) {
        double interaction = profile.getInteractionLevel() != null ? profile.getInteractionLevel() : 0;
        double practicality = profile.getPracticality() != null ? profile.getPracticality() : 0;
        double innovation = profile.getInnovation() != null ? profile.getInnovation() : 0;

        if (interaction >= 70) return "interactive";
        if (practicality >= 70) return "practical";
        if (innovation >= 70) return "innovative";
        return "academic";
    }
}