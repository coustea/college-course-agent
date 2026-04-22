package com.ccut.service.Impl;

import com.ccut.dto.BehaviorAnalysisResult;
import com.ccut.dto.CourseEvaluationSummary;
import com.ccut.dto.EvaluationResult;
import com.ccut.entity.*;
import com.ccut.mapper.*;
import com.ccut.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 学情报告服务实现类
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private LearningProgressMapper learningProgressMapper;

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private DocumentProgressMapper documentProgressMapper;

    @Autowired
    private WeeklyStudyTimeMapper weeklyStudyTimeMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private IdeologyResourceMapper ideologyResourceMapper;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private BehaviorAnalysisService behaviorAnalysisService;

    @Override
    public StudentLearningReport generateStudentReport(Long studentId, Long courseId) {
        log.info("生成学生学情报告: studentId={}, courseId={}", studentId, courseId);

        Student student = studentMapper.selectById(studentId);
        Course course = courseMapper.selectById(courseId);
        LearningProgress progress = learningProgressMapper.findOne(studentId, courseId);

        StudentLearningReport report = new StudentLearningReport();
        report.setStudentId(studentId);
        report.setStudentName(student != null ? student.getName() : "");
        report.setClassName(student != null ? student.getClassName() : "");
        report.setCourseId(courseId);
        report.setCourseName(course != null ? course.getCourseName() : "");

        // 学习进度
        if (progress != null) {
            report.setCompletionRate(progress.getCompletionPercentage());
            report.setTotalStudyMinutes(progress.getTimeSpent() != null ? progress.getTimeSpent() / 60 : 0);
        } else {
            report.setCompletionRate(0.0);
            report.setTotalStudyMinutes(0);
        }

        // 视频和文档统计
        List<VideoProgress> videoProgresses = videoProgressMapper.findByStudentAndCourse(studentId, courseId);
        report.setVideoWatchCount(videoProgresses != null ? videoProgresses.size() : 0);

        List<DocumentProgress> docProgresses = documentProgressMapper.findByStudentAndCourse(studentId, courseId);
        report.setDocumentReadCount(docProgresses != null ? docProgresses.size() : 0);

        // 评价数据
        try {
            EvaluationResult evaluation = evaluationService.evaluateStudent(studentId, courseId, "weekly");
            report.setEvaluation(evaluation);
        } catch (Exception e) {
            log.warn("获取评价数据失败: {}", e.getMessage());
        }

        // 思政学习情况
        List<IdeologyResource> ideologyResources = ideologyResourceMapper.search(courseId, null, "published", 100);
        report.setIdeologyResourceViewed(ideologyResources != null ? ideologyResources.size() : 0);

        // 行为画像
        try {
            BehaviorAnalysisResult behavior = behaviorAnalysisService.analyzeStudentBehavior(studentId);
            report.setPreferredStudyTime(behavior.getPreferredStudyTime());
            report.setLearningStyle(behavior.getLearningStyle());
            report.setEngagementLevel(behavior.getEngagementLevel());
            report.setIdeologyEngagementRate(behavior.getIdeologyEngagementRate());
        } catch (Exception e) {
            log.warn("获取行为画像失败: {}", e.getMessage());
        }

        // 生成建议
        report.setSuggestions(generateSuggestions(report));

        // 周趋势
        report.setWeeklyTrend(getWeeklyTrend(studentId, courseId));

        return report;
    }

    @Override
    public CourseLearningReport generateCourseReport(Long courseId, String period) {
        log.info("生成课程学情报告: courseId={}, period={}", courseId, period);

        Course course = courseMapper.selectById(courseId);
        List<Long> studentIds = enrollmentMapper.findStudentIdsByCourseId(courseId);

        CourseLearningReport report = new CourseLearningReport();
        report.setCourseId(courseId);
        report.setCourseName(course != null ? course.getCourseName() : "");
        report.setTotalStudents(studentIds.size());

        // 计算活跃学生数和平均完成率
        int activeCount = 0;
        double totalCompletion = 0;
        int totalStudyMinutes = 0;

        for (Long studentId : studentIds) {
            LearningProgress progress = learningProgressMapper.findOne(studentId, courseId);
            if (progress != null) {
                if (progress.getTimeSpent() != null && progress.getTimeSpent() > 0) {
                    activeCount++;
                    totalStudyMinutes += progress.getTimeSpent() / 60;
                }
                if (progress.getCompletionPercentage() != null) {
                    totalCompletion += progress.getCompletionPercentage();
                }
            }
        }

        report.setActiveStudents(activeCount);
        report.setAvgCompletionRate(studentIds.isEmpty() ? 0 : totalCompletion / studentIds.size());
        report.setAvgStudyHours(activeCount == 0 ? 0 : totalStudyMinutes / 60.0 / activeCount);

        // 评价汇总
        try {
            CourseEvaluationSummary evalSummary = evaluationService.getCourseSummary(courseId, period);
            report.setEvaluationSummary(evalSummary);
            report.setWarnings(evalSummary.getWarnings());
            report.setScoreDistribution(evalSummary.getScoreDistribution());
        } catch (Exception e) {
            log.warn("获取评价汇总失败: {}", e.getMessage());
        }

        // 思政资源情况
        List<IdeologyResource> ideologyResources = ideologyResourceMapper.search(courseId, null, "published", 100);
        report.setIdeologyResourceCount(ideologyResources != null ? ideologyResources.size() : 0);

        return report;
    }

    @Override
    public WeeklyReport getStudentWeeklyReport(Long studentId, Long courseId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        WeeklyReport report = new WeeklyReport();
        report.setWeekStart(weekStart.toString());

        // 获取本周学习时间
        List<WeeklyStudyTime> studyTimes = weeklyStudyTimeMapper.findByStudentAndCourse(studentId, courseId);
        if (studyTimes != null && !studyTimes.isEmpty()) {
            report.setStudyMinutes(studyTimes.get(0).getTotalSeconds() / 60);
        } else {
            report.setStudyMinutes(0);
        }

        // TODO: 其他周报数据

        return report;
    }

    @Override
    public List<StudentLearningReport> batchGenerateStudentReports(Long courseId) {
        List<Long> studentIds = enrollmentMapper.findStudentIdsByCourseId(courseId);
        List<StudentLearningReport> reports = new ArrayList<>();

        for (Long studentId : studentIds) {
            try {
                reports.add(generateStudentReport(studentId, courseId));
            } catch (Exception e) {
                log.error("生成学生报告失败: studentId={}, error={}", studentId, e.getMessage());
            }
        }

        return reports;
    }

    private List<String> generateSuggestions(StudentLearningReport report) {
        List<String> suggestions = new ArrayList<>();

        if (report.getCompletionRate() != null && report.getCompletionRate() < 50) {
            suggestions.add("建议增加学习时间，提高课程完成进度");
        }

        if (report.getTotalStudyMinutes() != null && report.getTotalStudyMinutes() < 60) {
            suggestions.add("本周学习时间较少，建议每天安排固定学习时间");
        }

        if (report.getIdeologyEngagementRate() != null && report.getIdeologyEngagementRate() < 0.3) {
            suggestions.add("建议多关注思政学习资源，提升综合素养");
        }

        if ("low".equals(report.getEngagementLevel())) {
            suggestions.add("学习参与度较低，建议积极参与课堂互动");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("学习状态良好，继续保持！");
        }

        return suggestions;
    }

    private List<DailyProgress> getWeeklyTrend(Long studentId, Long courseId) {
        List<DailyProgress> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DailyProgress dp = new DailyProgress();
            dp.setDate(date.toString());
            // TODO: 查询当天学习数据
            dp.setStudyMinutes(0);
            dp.setProgress(0.0);
            trend.add(dp);
        }

        return trend;
    }
}