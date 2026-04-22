package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 课程评价汇总DTO
 * 教师端查看课程整体评价数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseEvaluationSummary {
    // 课程信息
    private Long courseId;
    private String courseName;
    private String teacherName;
    private Integer studentCount;          // 选课学生总数

    // 课程整体评分
    private Double overallScore;           // 课程整体评分
    private Double contentIntegrationAvg;  // 内容融入度平均
    private Double interactionAvg;         // 师生互动平均
    private Double participationAvg;       // 学生参与度平均
    private Double valueRecognitionAvg;    // 价值认同平均

    // 评分分布
    private Map<String, Integer> scoreDistribution;  // 评分分布：{"优秀":10,"良好":15,"中等":5,"待提升":2}

    // 思政资源使用情况
    private Integer ideologyResourceCount;    // 思政资源总数
    private Integer ideologyViewCount;        // 思政资源查看次数
    private Double ideologyCoverageRate;      // 思政资源覆盖率

    // 学习行为统计
    private Double avgStudyHours;             // 平均学习时长
    private Integer totalInteractions;        // 总互动次数
    private Double avgCompletionRate;         // 平均完成率

    // 趋势数据
    private List<TrendPoint> weeklyTrends;    // 周度趋势数据

    // 问题学生预警
    private List<StudentWarning> warnings;    // 预警学生列表

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String week;           // 周标识
        private Double score;          // 评分
        private Integer activeUsers;   // 活跃用户数
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentWarning {
        private Long studentId;
        private String studentName;
        private String warningType;    // 预警类型：低参与度/低完成率/无思政互动
        private Double currentScore;
        private String suggestion;
    }
}