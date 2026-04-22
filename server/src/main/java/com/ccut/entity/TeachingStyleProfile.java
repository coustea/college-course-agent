package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教师授课风格分析实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeachingStyleProfile {
    private Long id;                    // 画像ID
    private Long teacherId;             // 教师ID

    // 授课风格维度
    private Double interactionLevel;    // 互动程度 (0-100)
    private Double contentDepth;        // 内容深度 (0-100)
    private Double practicality;        // 实用性 (0-100)
    private Double innovation;          // 创新性 (0-100)
    private Double ideologyIntegration; // 思政融入度 (0-100)

    // 思政教学特征
    private String ideologyApproach;    // 思政融入方式：implicit/explicit/mixed
    private String mainValueThemes;     // 主要价值主题（逗号分隔）
    private Integer ideologyResourceCount; // 思政资源使用数量

    // 教学效果
    private Double avgStudentSatisfaction; // 平均学生满意度
    private Double avgEngagementRate;   // 平均参与率

    // 风格标签
    private String styleTag;            // 风格标签：interactive/practical/academic/innovative

    private LocalDateTime lastAnalyzedAt; // 最后分析时间
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}