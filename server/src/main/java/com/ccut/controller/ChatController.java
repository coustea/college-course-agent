package com.ccut.controller;

import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import com.ccut.dto.Result;
import com.ccut.entity.Conversation;
import com.ccut.service.ChatService;
import com.ccut.service.ConversationService;
import com.ccut.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 聊天 Controller
 */
@RestController
@RequestMapping("/api/ai/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息（同步）
     * POST /api/ai/chat/send
     */
    @PostMapping("/send")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        // 参数验证：conversationId不能为空
        if (request.conversationId() == null || request.conversationId().trim().isEmpty()) {
            return Result.error(400, "conversationId 不能为空");
        }

        // 参数验证：消息内容不能为空
        if (request.message() == null || request.message().trim().isEmpty()) {
            return Result.error(400, "消息内容不能为空");
        }

        // 业务验证：会话必须存在
        Conversation conversation = conversationService.getConversation(request.conversationId());
        if (conversation == null) {
            return Result.error(404, "会话不存在");
        }

        // 业务验证：限制对话轮数（最多50轮=100条消息，防止token滥用）
        int messageCount = messageService.getMessageCount(request.conversationId());
        if (messageCount >= 100) {
            return Result.error(400, "当前会话已达到最大对话轮数（50轮），请创建新会话");
        }

        try {
            // 调用聊天服务处理请求（同步）
            ChatResponse chatResponse = chatService.chat(request);
            return Result.success(chatResponse);
        } catch (Exception e) {
            return Result.error(500, "聊天失败: " + e.getMessage());
        }
    }

    /**
     * 流式聊天接口（SSE服务端推送）
     * POST /api/ai/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        // 参数验证：conversationId不能为空
        if (request.conversationId() == null || request.conversationId().trim().isEmpty()) {
            return Flux.just("data: {\"code\": 400, \"message\": \"conversationId 不能为空\"}\n\n");
        }

        // 参数验证：消息内容不能为空
        if (request.message() == null || request.message().trim().isEmpty()) {
            return Flux.just("data: {\"code\": 400, \"message\": \"消息内容不能为空\"}\n\n");
        }

        // 业务验证：会话必须存在
        Conversation conversation = conversationService.getConversation(request.conversationId());
        if (conversation == null) {
            return Flux.just("data: {\"code\": 404, \"message\": \"会话不存在\"}\n\n");
        }

        // 业务验证：限制对话轮数
        int messageCount = messageService.getMessageCount(request.conversationId());
        if (messageCount >= 100) {
            return Flux.just("data: {\"code\": 400, \"message\": \"当前会话已达到最大对话轮数（50轮），请创建新会话\", \"messageCount\": " + messageCount + ", \"maxRounds\": 50}\n\n");
        }

        try {
            // 调用流式聊天服务（返回SSE流）
            return chatService.chatStream(request)
                    .map(chunk -> "data: " + chunk + "\n\n") // 包装SSE格式
                    .onErrorResume(e -> Flux.just("data: {\"code\": 500, \"message\": \"" + e.getMessage() + "\"}\n\n"));
        } catch (Exception e) {
            return Flux.just("data: {\"code\": 500, \"message\": \"聊天失败: " + e.getMessage() + "\"}\n\n");
        }
    }
}
