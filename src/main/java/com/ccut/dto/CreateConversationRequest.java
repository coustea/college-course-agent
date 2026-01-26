package com.ccut.dto;

import lombok.Data;

/**
 * 创建会话请求 DTO
 */
@Data
public class CreateConversationRequest {
    /**
     * 用户名
     */
    private String username;

    /**
     * 会话标题（可选，默认"新对话"）
     */
    private String title;
}
