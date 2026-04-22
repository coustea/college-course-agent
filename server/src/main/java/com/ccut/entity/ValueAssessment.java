package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 价值认同评估实体
 * 追踪学生价值认同变化趋势
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValueAssessment {
    private Long id;                    // 评估ID
    private Long studentId;             // 学生ID
    private Long courseId;              // 课程ID

    // 价值认同维度得分
    private Double patriotismScore;     // 爱国主义认同 (0-100)
    private Double socialResponsibilityScore; // 社会责任认同 (0-100)
    private Double professionalEthicsScore; // 职业道德认同 (0-100)
    private Double innovationScore;     // 创新精神认同 (0-100)
    private Double culturalConfidenceScore; // 文化自信认同 (0-100)

    // 综合得分
    private Double totalScore;          // 总体认同度 (0-100)
    private String assessmentLevel;     // 评估等级：high/medium/low

    // 变化趋势
    private Double previousScore;       // 上期得分
    private String trendDirection;      // 趋势方向：up/stable/down
    private Double trendValue;          // 变化幅度

    // 评估依据
    private String assessmentBasis;     // 评估依据说明
    private String assessmentPeriod;    // 评估周期：weekly/monthly

    private LocalDateTime assessedAt;   // 评估时间
    private LocalDateTime createdAt;    // 创建时间
}