package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 行为分析结果DTO
 * 学生学习行为画像分析结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorAnalysisResult {
    // 学生信息
    private Long studentId;
    private String studentName;
    private String className;

    // 学习时段分析
    private String preferredStudyTime;        // 偏好时段
    private Map<String, Double> timeDistribution;  // 时段分布

    // 资源偏好分析
    private String preferredResourceType;     // 偏好资源类型
    private Map<String, Double> resourcePreference;  // 资源类型偏好度

    // 学习效率指标
    private Double avgSessionDuration;        // 平均学习时长
    private Integer totalStudyHours;          // 总学习时长
    private Double completionRate;            // 完成率
    private Double consistencyScore;          // 连贯性评分

    // 互动行为
    private Integer interactionCount;         // 互动次数
    private Integer aiChatCount;              // AI对话次数
    private Integer questionCount;            // 提问次数

    // 思政学习特征
    private Double ideologyEngagementRate;    // 思政参与率
    private List<String> ideologyThemes;      // 关注的思政主题

    // 学习风格分析
    private String learningStyle;             // 学习风格标签
    private String engagementLevel;           // 参与度等级

    // 行为-成效关联
    private Double behaviorEffectivenessScore;  // 行为有效性评分
    private String effectivenessAnalysis;       // 有效性分析说明

    // 改进建议
    private List<String> recommendations;     // 个性化学习建议

    // 同龄对比
    private Double peerAverageScore;          // 同班平均水平
    private String peerComparison;            // 与同龄人对比说明
}