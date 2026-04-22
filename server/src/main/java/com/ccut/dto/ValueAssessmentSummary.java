package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 价值认同评估汇总DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValueAssessmentSummary {
    private Long courseId;
    private String courseName;
    private Integer studentCount;
    private Double avgTotalScore;
    private Double avgPatriotismScore;
    private Double avgSocialResponsibilityScore;
    private Double avgProfessionalEthicsScore;
    private Double avgInnovationScore;
    private Double avgCulturalConfidenceScore;
    private List<ValueDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueDistribution {
        private String level;
        private Integer count;
        private Double percentage;
    }
}