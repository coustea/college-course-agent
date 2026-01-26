package com.ccut.dto;

import lombok.Data;

/**
 * 聊天请求 DTO
 */
@Data
public class ChatRequest {
    /**
     * 会话ID（格式：username:序号）
     */
    private String conversationId;

    /**
     * 用户消息内容
     */
    private String message;
}
