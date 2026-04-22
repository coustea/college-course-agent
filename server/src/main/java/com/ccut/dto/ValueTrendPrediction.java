package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 价值认同趋势预测DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValueTrendPrediction {
    private Long studentId;
    private String predictedTrend;  // up/stable/down
    private Double predictedScore;
    private Double confidence;
    private String recommendation;
    private List<TrendPoint> historicalTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String period;
        private Double score;
    }
}