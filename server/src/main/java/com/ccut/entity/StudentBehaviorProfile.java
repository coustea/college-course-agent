package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生行为画像实体
 * 记录学生学习行为特征，用于个性化推荐和分析
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentBehaviorProfile {
    private Long profileId;                 // 画像ID
    private Long studentId;                 // 学生ID

    // 学习时段偏好
    private String preferredStudyTime;      // 偏好学习时段：morning/afternoon/evening/night
    private Double morningRatio;            // 上午学习占比
    private Double afternoonRatio;          // 下午学习占比
    private Double eveningRatio;            // 晚上学习占比

    // 资源类型偏好
    private String preferredResourceType;   // 偏好资源类型：video/document/interactive
    private Double videoPreference;         // 视频偏好度
    private Double documentPreference;      // 文档偏好度
    private Double interactivePreference;   // 互动偏好度

    // 学习行为特征
    private Double avgSessionDuration;      // 平均学习时长（分钟）
    private Integer interactionFrequency;   // 互动频率（次数/周）
    private Double completionRate;          // 完成率
    private Double consistencyScore;        // 学习连贯性评分

    // 学习风格标签
    private String learningStyleTag;        // 学习风格：visual/auditory/kinesthetic/mixed
    private String engagementLevel;         // 参与度等级：high/medium/low

    // 思政学习特征
    private Double ideologyClickRate;       // 思政资源点击率
    private Double ideologyCompletionRate;  // 思政资源完成率

    private LocalDateTime lastAnalyzedAt;   // 最后分析时间
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
}