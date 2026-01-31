package com.ccut.service;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 处理聊天请求（同步）
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 处理聊天请求（流式）
     * @param request 聊天请求
     * @return 流式响应
     */
    Flux<String> chatStream(ChatRequest request) throws GraphRunnerException;
}
