package com.ccut.dto;

import lombok.Data;

import java.util.List;

@Data
public class IdeologyAnalysisResult {
    private String summary;
    private String valueTheme;
    private String applicableScene;
    private List<String> keywords;
    private List<String> tags;
    private String difficulty;
}
