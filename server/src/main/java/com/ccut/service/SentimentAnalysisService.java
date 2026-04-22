package com.ccut.service;

import com.ccut.dto.SentimentAnalysisResult;
import com.ccut.dto.SentimentDistribution;
import com.ccut.entity.SentimentRecord;

import java.util.List;

/**
 * 情感分析服务接口
 */
public interface SentimentAnalysisService {

    /**
     * 分析文本情感
     */
    SentimentAnalysisResult analyzeSentiment(String text);

    /**
     * 分析学生情感趋势
     */
    List<SentimentRecord> getStudentSentimentTrend(Long studentId, Long courseId, int days);

    /**
     * 记录情感分析结果
     */
    void recordSentiment(Long studentId, Long courseId, String sourceType,
                         String sourceContent, Long sourceId);

    /**
     * 获取课程整体情感分布
     */
    SentimentDistribution getCourseSentimentDistribution(Long courseId);

    /**
     * 分析思政相关情感
     */
    SentimentAnalysisResult analyzeIdeologySentiment(String text, String ideologyTheme);
}