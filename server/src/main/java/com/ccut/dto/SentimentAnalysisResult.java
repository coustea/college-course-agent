package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情感分析结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentAnalysisResult {
    // 情感类型
    private String sentimentType;       // positive/neutral/negative

    // 情感得分
    private Double positiveScore;      // 积极情感得分 (0-1)
    private Double negativeScore;      // 消极情感得分 (0-1)
    private Double neutralScore;       // 中性情感得分 (0-1)

    // 情感强度
    private Double intensity;          // 情感强度 (0-1)

    // 关键情感词
    private List<String> positiveWords;
    private List<String> negativeWords;

    // 思政相关
    private Boolean ideologyRelated;
    private String ideologyTheme;

    // 分析说明
    private String analysis;

    /**
     * 创建默认积极结果
     */
    public static SentimentAnalysisResult positive() {
        SentimentAnalysisResult result = new SentimentAnalysisResult();
        result.setSentimentType("positive");
        result.setPositiveScore(0.7);
        result.setNegativeScore(0.1);
        result.setNeutralScore(0.2);
        result.setIntensity(0.6);
        return result;
    }

    /**
     * 创建默认中性结果
     */
    public static SentimentAnalysisResult neutral() {
        SentimentAnalysisResult result = new SentimentAnalysisResult();
        result.setSentimentType("neutral");
        result.setPositiveScore(0.2);
        result.setNegativeScore(0.2);
        result.setNeutralScore(0.6);
        result.setIntensity(0.3);
        return result;
    }
}
