package com.ccut.dto;

import lombok.Data;

@Data
public class IdeologyResourceRequest {
    private Long courseId;
    private Long chapterId;
    private Long videoId;
    private Long documentId;
    private String title;
    private String resourceType;
    private String contentSummary;
    private String sourceUrl;
    private String valueTheme;
    private String applicableScene;
    private String keywords;
    private String difficulty;
    private String status;
    private Long createdBy;
    private Boolean autoAnalyze;
}
