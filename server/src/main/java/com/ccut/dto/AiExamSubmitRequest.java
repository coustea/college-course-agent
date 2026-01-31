package com.ccut.dto;

import java.util.List;

/**
 * AI考试提交请求
 */
public record AiExamSubmitRequest(Long examId, Long studentId, List<AiExamSubmitAnswer> answers) {}
