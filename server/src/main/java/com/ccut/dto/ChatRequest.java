package com.ccut.dto;

/**
 * 聊天请求 DTO
 */
public record ChatRequest(
    /**
     * 会话ID（格式：username:序号）
     */
    String conversationId,

    /**
     * 用户消息内容
     */
    String message
) {}
