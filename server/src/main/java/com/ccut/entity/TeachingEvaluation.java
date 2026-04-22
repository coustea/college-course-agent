package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教学评价实体
 * 多维度教学评价系统：内容融入度、师生互动、学生参与度、价值认同
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeachingEvaluation {
    private Long evaluationId;          // 评价ID
    private Long courseId;              // 课程ID
    private Long studentId;             // 学生ID（null表示课程整体评价）
    private Long teacherId;             // 教师ID

    // 四个维度评分 (0-100)
    private Double contentIntegrationScore;    // 内容融入度：思政资源与课程的融合程度
    private Double interactionScore;           // 师生互动：互动频率与质量
    private Double participationScore;         // 学生参与度：学习活跃度
    private Double valueRecognitionScore;      // 价值认同：思政内容接受度

    private Double totalScore;          // 综合评分
    private String evaluationPeriod;    // 评价周期：weekly/monthly/semester
    private String evaluationType;      // 评价类型：course/student/teacher
    private String remark;              // 评价备注
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}