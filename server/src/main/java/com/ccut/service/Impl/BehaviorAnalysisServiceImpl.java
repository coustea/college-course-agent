package com.ccut.service.Impl;

import com.ccut.dto.BehaviorAnalysisResult;
import com.ccut.entity.*;
import com.ccut.mapper.*;
import com.ccut.service.BehaviorAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 行为分析服务实现类
 * 学生学习行为画像分析
 */
@Slf4j
@Service
public class BehaviorAnalysisServiceImpl implements BehaviorAnalysisService {

    @Autowired
    private StudentBehaviorProfileMapper profileMapper;

    @Autowired
    private LearningPathRecordMapper pathRecordMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private LearningProgressMapper learningProgressMapper;

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private DocumentProgressMapper documentProgressMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private IdeologyResourceRecommendationMapper ideologyRecommendationMapper;

    @Override
    public BehaviorAnalysisResult analyzeStudentBehavior(Long studentId) {
        log.info("分析学生行为: studentId={}", studentId);

        Student student = studentMapper.selectById(studentId);
        StudentBehaviorProfile profile = getOrCreateProfile(studentId);

        // 分析学习时段偏好
        String preferredTime = analyzePreferredStudyTime(studentId);
        profile.setPreferredStudyTime(preferredTime);

        // 分析资源类型偏好
        String preferredResource = analyzePreferredResourceType(studentId);
        profile.setPreferredResourceType(preferredResource);

        // 计算各项指标
        calculateBehaviorMetrics(profile, studentId);

        // 保存画像
        profile.setLastAnalyzedAt(LocalDateTime.now());
        profileMapper.upsert(profile);

        // 构建返回结果
        return buildAnalysisResult(profile, student);
    }

    @Override
    public List<BehaviorAnalysisResult> analyzeCourseBehavior(Long courseId) {
        List<Student> students = enrollmentMapper.findStudentsByCourseId(courseId);
        if (students == null || students.isEmpty()) {
            return new ArrayList<>();
        }

        List<BehaviorAnalysisResult> results = new ArrayList<>();
        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            results.add(analyzeStudentBehavior(student.getId()));
        }
        return results;
    }

    @Override
    public List<LearningPathRecord> getLearningPath(Long studentId, Long courseId, Integer limit) {
        return pathRecordMapper.selectByStudentId(studentId, courseId, limit != null ? limit : 50);
    }

    @Override
    @Transactional
    public void recordLearningAction(Long studentId, Long courseId, String resourceType,
                                     Long resourceId, String resourceTitle, String actionType,
                                     Integer durationSeconds, Double progressPercent) {
        LearningPathRecord record = new LearningPathRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        record.setResourceTitle(resourceTitle);
        record.setActionType(actionType);
        record.setDurationSeconds(durationSeconds != null ? durationSeconds : 0);
        record.setProgressPercent(progressPercent != null ? progressPercent : 0.0);
        record.setCreatedAt(LocalDateTime.now());

        pathRecordMapper.insert(record);
        log.debug("记录学习行为: studentId={}, type={}, action={}", studentId, resourceType, actionType);
    }

    @Override
    @Transactional
    public StudentBehaviorProfile updateBehaviorProfile(Long studentId) {
        StudentBehaviorProfile profile = getOrCreateProfile(studentId);

        profile.setPreferredStudyTime(analyzePreferredStudyTime(studentId));
        profile.setPreferredResourceType(analyzePreferredResourceType(studentId));
        calculateBehaviorMetrics(profile, studentId);
        profile.setLastAnalyzedAt(LocalDateTime.now());

        profileMapper.upsert(profile);
        return profile;
    }

    @Override
    public Double calculateBehaviorEffectiveness(Long studentId, Long courseId) {
        // 计算行为有效性：行为特征与学习成效的关联
        StudentBehaviorProfile profile = profileMapper.selectByStudentId(studentId);
        if (profile == null) {
            return 50.0;
        }

        LearningProgress progress = learningProgressMapper.findOne(studentId, courseId);
        if (progress == null) {
            return 50.0;
        }

        // 综合学习连贯性、完成率、互动频率等计算有效性
        double score = 0;
        score += (profile.getConsistencyScore() != null ? profile.getConsistencyScore() : 50) * 0.3;
        score += (profile.getCompletionRate() != null ? profile.getCompletionRate() * 100 : 50) * 0.4;
        score += Math.min((profile.getInteractionFrequency() != null ? profile.getInteractionFrequency() : 0) * 2, 30);

        return Math.min(score, 100);
    }

    @Override
    public StudentBehaviorProfile getStudentProfile(Long studentId) {
        return profileMapper.selectByStudentId(studentId);
    }

    @Override
    public void batchUpdateCourseProfiles(Long courseId) {
        List<Student> students = enrollmentMapper.findStudentsByCourseId(courseId);
        if (students == null || students.isEmpty()) {
            return;
        }

        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            updateBehaviorProfile(student.getId());
        }
    }

    @Override
    public String analyzePreferredStudyTime(Long studentId) {
        // 分析学习时段偏好
        List<LearningPathRecord> records = pathRecordMapper.selectRecentByStudentId(studentId, 100);
        if (records == null || records.isEmpty()) {
            return "unknown";
        }

        int morning = 0, afternoon = 0, evening = 0, night = 0;
        for (LearningPathRecord record : records) {
            if (record.getCreatedAt() == null) continue;
            LocalTime time = record.getCreatedAt().toLocalTime();
            int hour = time.getHour();

            if (hour >= 6 && hour < 12) morning++;
            else if (hour >= 12 && hour < 18) afternoon++;
            else if (hour >= 18 && hour < 22) evening++;
            else night++;
        }

        int max = Math.max(Math.max(morning, afternoon), Math.max(evening, night));
        if (max == morning) return "morning";
        if (max == afternoon) return "afternoon";
        if (max == evening) return "evening";
        return "night";
    }

    @Override
    public String analyzePreferredResourceType(Long studentId) {
        // 分析资源类型偏好
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        int videoCount = pathRecordMapper.countByStudentAndResource(studentId, "video", weekAgo);
        int docCount = pathRecordMapper.countByStudentAndResource(studentId, "document", weekAgo);
        int ideologyCount = pathRecordMapper.countByStudentAndResource(studentId, "ideology", weekAgo);

        int max = Math.max(Math.max(videoCount, docCount), ideologyCount);
        if (max == 0) return "mixed";
        if (max == videoCount) return "video";
        if (max == docCount) return "document";
        return "interactive";
    }

    private StudentBehaviorProfile getOrCreateProfile(Long studentId) {
        StudentBehaviorProfile profile = profileMapper.selectByStudentId(studentId);
        if (profile == null) {
            profile = new StudentBehaviorProfile();
            profile.setStudentId(studentId);
            profile.setCreatedAt(LocalDateTime.now());
        }
        return profile;
    }

    private void calculateBehaviorMetrics(StudentBehaviorProfile profile, Long studentId) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        List<LearningPathRecord> recentRecords = pathRecordMapper.selectByStudentId(studentId, null, 100);
        if (recentRecords == null) {
            recentRecords = new ArrayList<>();
        }

        // 计算平均学习时长
        Integer totalDuration = pathRecordMapper.sumDurationByStudent(studentId, null, weekAgo);
        int totalMinutes = (totalDuration != null ? totalDuration : 0) / 60;
        int sessionCount = pathRecordMapper.countByStudentAndResource(studentId, null, weekAgo);
        profile.setAvgSessionDuration(sessionCount > 0 ? (double) totalMinutes / sessionCount : 0.0);

        // 计算互动频率
        List<Conversation> conversations = conversationMapper.findByStudentId(studentId);
        int interactionFrequency = conversations != null ? conversations.size() : 0;
        profile.setInteractionFrequency(interactionFrequency);

        // 计算完成率
        profile.setCompletionRate(calculateCompletionRate(studentId, recentRecords));

        // 计算学习连贯性
        profile.setConsistencyScore(calculateConsistencyScore(studentId, recentRecords));

        // 确定学习风格
        profile.setLearningStyleTag(determineLearningStyle(profile));

        // 确定参与度等级
        profile.setEngagementLevel(determineEngagementLevel(profile));

        // 计算思政参与率
        calculateIdeologyMetrics(profile, studentId);
    }

    private Double calculateConsistencyScore(Long studentId, List<LearningPathRecord> recentRecords) {
        int consecutiveDays = 0;
        try {
            Integer value = learningProgressMapper.getConsecutiveDays(studentId);
            consecutiveDays = value != null ? value : 0;
        } catch (Exception e) {
            log.warn("读取连续学习天数失败: {}", e.getMessage());
        }

        Set<java.time.LocalDate> activeDays = new HashSet<>();
        for (LearningPathRecord record : recentRecords) {
            if (record.getCreatedAt() != null) {
                activeDays.add(record.getCreatedAt().toLocalDate());
            }
        }

        int activeDayScore = Math.min(activeDays.size() * 12, 48);
        int streakScore = Math.min(consecutiveDays * 8, 32);
        int recordScore = Math.min(recentRecords.size() * 3, 20);
        return Math.min((double) activeDayScore + streakScore + recordScore, 100.0);
    }

    private Double calculateCompletionRate(Long studentId, List<LearningPathRecord> recentRecords) {
        double recordCompletionRate = 0.0;
        if (!recentRecords.isEmpty()) {
            long completedActions = recentRecords.stream()
                    .filter(record -> {
                        String actionType = record.getActionType();
                        Double progress = record.getProgressPercent();
                        return (actionType != null && ("complete".equalsIgnoreCase(actionType) || "review".equalsIgnoreCase(actionType)))
                                || (progress != null && progress >= 95.0);
                    })
                    .count();
            recordCompletionRate = (double) completedActions / recentRecords.size();
        }

        double weeklyStudyScore = 0.0;
        try {
            Integer weeklyStudyTime = learningProgressMapper.getWeeklyStudyTime(studentId);
            weeklyStudyScore = Math.min((weeklyStudyTime != null ? weeklyStudyTime : 0) / 25200.0, 1.0);
        } catch (Exception e) {
            log.warn("读取周学习时长失败: {}", e.getMessage());
        }

        double completionRate = recordCompletionRate * 0.7 + weeklyStudyScore * 0.3;
        return Math.max(0.0, Math.min(completionRate, 1.0));
    }

    private String determineLearningStyle(StudentBehaviorProfile profile) {
        double videoPref = profile.getVideoPreference() != null ? profile.getVideoPreference() : 0.33;
        double docPref = profile.getDocumentPreference() != null ? profile.getDocumentPreference() : 0.33;
        double interPref = profile.getInteractivePreference() != null ? profile.getInteractivePreference() : 0.33;

        if (videoPref > 0.5) return "visual";
        if (docPref > 0.5) return "reading";
        if (interPref > 0.5) return "kinesthetic";
        return "mixed";
    }

    private String determineEngagementLevel(StudentBehaviorProfile profile) {
        double completionRate = profile.getCompletionRate() != null ? profile.getCompletionRate() : 0;
        int interaction = profile.getInteractionFrequency() != null ? profile.getInteractionFrequency() : 0;

        double score = completionRate * 60 + Math.min(interaction * 2, 40);
        if (score >= 70) return "high";
        if (score >= 40) return "medium";
        return "low";
    }

    private void calculateIdeologyMetrics(StudentBehaviorProfile profile, Long studentId) {
        List<IdeologyResourceRecommendation> recommendations =
                ideologyRecommendationMapper.findByStudentId(studentId, 100);

        if (recommendations != null && !recommendations.isEmpty()) {
            int clicked = (int) recommendations.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getHasClicked()))
                    .count();
            profile.setIdeologyClickRate((double) clicked / recommendations.size());
        } else {
            profile.setIdeologyClickRate(0.0);
        }
        profile.setIdeologyCompletionRate(profile.getIdeologyClickRate());
    }

    private BehaviorAnalysisResult buildAnalysisResult(StudentBehaviorProfile profile, Student student) {
        BehaviorAnalysisResult result = new BehaviorAnalysisResult();
        result.setStudentId(profile.getStudentId());
        result.setStudentName(student != null ? student.getName() : "未知");
        result.setClassName(student != null ? student.getClassName() : "");

        result.setPreferredStudyTime(profile.getPreferredStudyTime());
        result.setPreferredResourceType(profile.getPreferredResourceType());
        result.setAvgSessionDuration(profile.getAvgSessionDuration());
        result.setCompletionRate(profile.getCompletionRate());
        result.setConsistencyScore(profile.getConsistencyScore());
        result.setInteractionCount(profile.getInteractionFrequency());
        result.setAiChatCount(profile.getInteractionFrequency());
        Integer weeklyStudyTime = learningProgressMapper.getWeeklyStudyTime(profile.getStudentId());
        result.setTotalStudyHours(weeklyStudyTime != null ? weeklyStudyTime / 3600 : null);
        result.setLearningStyle(profile.getLearningStyleTag());
        result.setEngagementLevel(profile.getEngagementLevel());
        result.setIdeologyEngagementRate(profile.getIdeologyClickRate());

        // 生成建议
        List<String> recommendations = new ArrayList<>();
        if ("low".equals(profile.getEngagementLevel())) {
            recommendations.add("建议增加学习频率，提高学习参与度");
        }
        if (profile.getCompletionRate() != null && profile.getCompletionRate() < 0.5) {
            recommendations.add("建议按时完成课程任务，提高完成率");
        }
        if (profile.getIdeologyClickRate() != null && profile.getIdeologyClickRate() < 0.3) {
            recommendations.add("建议多关注思政学习资源，提升综合素养");
        }
        result.setRecommendations(recommendations);

        return result;
    }
}
