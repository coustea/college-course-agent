package com.ccut.service;

import com.ccut.dto.CourseEvaluationSummary;
import com.ccut.dto.EvaluationResult;

import java.util.List;
import java.util.Map;

/**
 * 学情报告服务接口
 */
public interface ReportService {

    /**
     * 生成学生学习报告
     */
    StudentLearningReport generateStudentReport(Long studentId, Long courseId);

    /**
     * 生成课程学情报告
     */
    CourseLearningReport generateCourseReport(Long courseId, String period);

    /**
     * 获取学生周报数据
     */
    WeeklyReport getStudentWeeklyReport(Long studentId, Long courseId);

    /**
     * 批量生成课程学生报告
     */
    List<StudentLearningReport> batchGenerateStudentReports(Long courseId);

    /**
     * 学生学习报告DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class StudentLearningReport {
        private Long studentId;
        private String studentName;
        private String className;
        private Long courseId;
        private String courseName;

        // 学习进度
        private Double completionRate;
        private Integer totalStudyMinutes;
        private Integer videoWatchCount;
        private Integer documentReadCount;

        // 学习评价
        private EvaluationResult evaluation;

        // 思政学习
        private Integer ideologyResourceViewed;
        private Double ideologyEngagementRate;

        // 学习行为
        private String preferredStudyTime;
        private String learningStyle;
        private String engagementLevel;

        // 建议
        private List<String> suggestions;

        // 趋势
        private List<DailyProgress> weeklyTrend;
    }

    /**
     * 课程学习报告DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class CourseLearningReport {
        private Long courseId;
        private String courseName;
        private String teacherName;

        // 课程统计
        private Integer totalStudents;
        private Integer activeStudents;
        private Double avgCompletionRate;
        private Double avgStudyHours;

        // 评价汇总
        private CourseEvaluationSummary evaluationSummary;

        // 思政融入情况
        private Integer ideologyResourceCount;
        private Double ideologyCoverageRate;

        // 预警学生
        private List<CourseEvaluationSummary.StudentWarning> warnings;

        // 分数分布
        private Map<String, Integer> scoreDistribution;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class WeeklyReport {
        private String weekStart;
        private Integer studyMinutes;
        private Integer videosCompleted;
        private Integer documentsRead;
        private Integer aiInteractions;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class DailyProgress {
        private String date;
        private Integer studyMinutes;
        private Double progress;
    }
}