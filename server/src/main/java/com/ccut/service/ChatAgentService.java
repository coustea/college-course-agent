package com.ccut.service;

import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 聊天 Agent 接口
 * 所有 AI 对话逻辑统一通过 ChatAgent 实现
 * Agent 工作流：思考(Think) → 规划(Plan) → 反思(Reflect) → 回复(Reply)
 */
public interface ChatAgentService {

    /**
     * 同步聊天
     * @param request 聊天请求（含消息、会话ID、附件）
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天（SSE）
     * @param request 聊天请求
     * @return 流式文本响应
     */
    Flux<String> chatStream(ChatRequest request);
}
