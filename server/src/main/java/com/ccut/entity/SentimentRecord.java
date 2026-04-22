package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 情感分析记录实体
 * 记录学生情感状态变化
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentRecord {
    private Long id;                    // 记录ID
    private Long studentId;             // 学生ID
    private Long courseId;              // 课程ID

    // 情感分析结果
    private String sentimentType;       // 情感类型：positive/neutral/negative
    private Double positiveScore;       // 积极情感得分 (0-1)
    private Double negativeScore;       // 消极情感得分 (0-1)
    private Double neutralScore;        // 中性情感得分 (0-1)

    // 分析来源
    private String sourceType;          // 来源类型：chat/reflection/discussion
    private String sourceContent;       // 来源内容（文本摘要）
    private Long sourceId;              // 来源ID

    // 思政相关
    private Boolean ideologyRelated;    // 是否与思政相关
    private String ideologyTheme;       // 思政主题标签

    private LocalDateTime analyzedAt;   // 分析时间
    private LocalDateTime createdAt;    // 创建时间
}
