package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情感分布DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentDistribution {
    private Double positiveRatio;
    private Double negativeRatio;
    private Double neutralRatio;
    private Integer totalRecords;
    private List<TrendPoint> dailyTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private Double avgPositiveScore;
        private Integer recordCount;
    }
}