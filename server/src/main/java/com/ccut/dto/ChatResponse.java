package com.ccut.dto;

import com.ccut.entity.Message;
import lombok.Data;

import java.util.List;

/**
 * 聊天响应 DTO
 */
@Data
public class ChatResponse {
    /**
     * 用户消息
     */
    private Message userMessage;

    /**
     * AI 消息
     */
    private Message aiMessage;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * AI 生成文件
     */
    private List<GeneratedFileInfo> generatedFiles;
}
