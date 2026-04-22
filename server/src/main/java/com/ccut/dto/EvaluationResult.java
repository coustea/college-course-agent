package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评价结果DTO
 * 包含各维度评分和综合分析
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {
    // 基础信息
    private Long evaluationId;
    private Long courseId;
    private String courseName;
    private Long studentId;
    private String studentName;

    // 四维度评分
    private Double contentIntegrationScore;    // 内容融入度 (0-100)
    private Double interactionScore;           // 师生互动 (0-100)
    private Double participationScore;         // 学生参与度 (0-100)
    private Double valueRecognitionScore;      // 价值认同 (0-100)

    // 综合评分
    private Double totalScore;                 // 综合评分 (0-100)
    private String evaluationLevel;            // 评价等级：优秀/良好/中等/待提升

    // 详细分析
    private String contentAnalysis;            // 内容融入分析说明
    private String interactionAnalysis;        // 互动分析说明
    private String participationAnalysis;      // 参与度分析说明
    private String valueAnalysis;              // 价值认同分析说明

    // 建议与改进方向
    private List<String> improvementSuggestions;   // 改进建议列表

    // 对比数据
    private Double classAverageScore;          // 班级平均分
    private Double previousScore;              // 上期评分（用于对比变化）
}