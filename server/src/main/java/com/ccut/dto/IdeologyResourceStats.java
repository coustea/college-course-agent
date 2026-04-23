package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdeologyResourceStats {
    private Long targetCount;
    private Long totalCount;
    private Long publishedCount;
    private Long draftCount;
    private Long pendingSourceCount;
    private Long courseCoverageCount;
    private Double completionRate;
    private Double publishedRate;
    private Map<String, Long> resourceTypeCounts = new LinkedHashMap<>();
    private Map<String, Long> valueThemeCounts = new LinkedHashMap<>();
    private Map<String, Long> applicableSceneCounts = new LinkedHashMap<>();
}
