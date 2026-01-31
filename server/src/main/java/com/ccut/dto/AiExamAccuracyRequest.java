package com.ccut.dto;

/**
 * AI考试正确率查询请求
 */
public record AiExamAccuracyRequest(Long studentId, Long courseId) {}
