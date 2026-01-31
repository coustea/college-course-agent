package com.ccut.dto;

/**
 * AI考试生成请求
 */
public record AiExamGenerateRequest(Long courseId, Long studentId, Integer choiceCount, Integer judgeCount, Long videoId, Long documentId) {}
