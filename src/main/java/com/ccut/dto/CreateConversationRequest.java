package com.ccut.dto;

/**
 * 创建会话请求 DTO
 */
public record CreateConversationRequest(
    /**
     * 用户名
     */
    String username,

    /**
     * 会话标题（可选，默认"新对话"）
     */
    String title
) {}
