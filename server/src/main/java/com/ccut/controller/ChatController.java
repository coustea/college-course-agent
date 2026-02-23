package com.ccut.controller;

import com.ccut.dto.Attachment;
import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import com.ccut.dto.Result;
import com.ccut.entity.Message;
import com.ccut.service.ChatAgentService;
import com.ccut.service.FileStorageService;
import com.ccut.service.MessageService;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天 Controller
 * 所有对话统一走 ChatAgent，默认支持文件/图片上传
 */
@RestController
@RequestMapping("/api/ai/chat")
public class ChatController {

    @Autowired
    private ChatAgentService chatAgentService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 同步聊天（支持可选文件上传）
     * POST /api/ai/chat/send
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ChatResponse> chat(
            @RequestPart("conversationId") String conversationId,
            @RequestPart("message") String message,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request) {

        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Result.error(400, "conversationId 不能为空");
        }
        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息内容不能为空");
        }

        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            System.out.println("========== [Chat Controller - Send] ==========");
            System.out.println("Username from JWT: " + username);
            System.out.println("ConversationId: " + conversationId);

            List<Attachment> attachments = saveFiles(files);
            ChatRequest chatRequest = new ChatRequest(conversationId, message, attachments);
            ChatResponse chatResponse = chatAgentService.chat(chatRequest, username);
            return Result.success(chatResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "聊天失败: " + e.getMessage());
        }
    }

    /**
     * 流式聊天（SSE，支持可选文件上传）
     * POST /api/ai/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<String> chatStream(
            @RequestPart("conversationId") String conversationId,
            @RequestPart("message") String message,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request) {

        if (conversationId == null || conversationId.trim().isEmpty()) {
            return Flux.just("{\"code\": 400, \"message\": \"conversationId 不能为空\"}");
        }
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("{\"code\": 400, \"message\": \"消息内容不能为空\"}");
        }

        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Flux.just("{\"code\": 401, \"message\": \"未授权访问\"}");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Flux.just("{\"code\": 401, \"message\": \"无法获取用户信息\"}");
            }

            System.out.println("========== [Chat Controller - Stream] ==========");
            System.out.println("Username from JWT: " + username);
            System.out.println("ConversationId: " + conversationId);

            List<Attachment> attachments = saveFiles(files);
            ChatRequest chatRequest = new ChatRequest(conversationId, message, attachments);
            System.out.println("[ChatController] 开始流式响应，username: " + username + ", conversationId: " + conversationId);
            return chatAgentService.chatStream(chatRequest, username)
                    .doOnNext(chunk -> {
                        System.out.println("[ChatController] 发送SSE chunk: " + chunk.substring(0, Math.min(100, chunk.length())));
                    })
                    .onErrorResume(e -> {
                        System.err.println("[ChatController] 流式响应错误: " + e.getMessage());
                        return Flux.just("{\"code\": 500, \"message\": \"" + e.getMessage() + "\"}");
                    })
                    .doOnComplete(() -> {
                        System.out.println("[ChatController] SSE流发送完成");
                    });
        } catch (Exception e) {
            System.err.println("[ChatController] 请求处理失败: " + e.getMessage());
            return Flux.just("{\"code\": 500, \"message\": \"聊天失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 获取聊天记录
     * GET /api/ai/chat/history
     * 从JWT token中获取当前登录用户的用户名，确保只能查看自己的聊天记录
     */
    @GetMapping("/history")
    public Result<List<Message>> getChatHistory(HttpServletRequest request) {
        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            // 只查询当前登录用户的消息
            List<Message> messages = messageService.findByUsername(username);
            return Result.success(messages);
        } catch (Exception e) {
            return Result.error(500, "获取聊天记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除聊天记录
     * DELETE /api/ai/chat/delete
     * 从JWT token中获取当前登录用户的用户名，确保只能删除自己的聊天记录
     */
    @DeleteMapping("/delete")
    public Result<String> deleteChat(HttpServletRequest request) {
        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            // 只删除当前登录用户的消息
            List<Message> messages = messageService.findByUsername(username);
            if (!messages.isEmpty()) {
                String conversationId = messages.get(0).getConversationId();
                messageService.deleteMessages(conversationId, username);
            }
            return Result.success("聊天记录删除成功");
        } catch (Exception e) {
            return Result.error(500, "删除聊天记录失败: " + e.getMessage());
        }
    }

    /**
     * 测试AI连接（用于调试）
     * GET /api/ai/chat/test
     */
    @GetMapping("/test")
    public Result<Map<String, Object>> testAIConnection(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 从JWT token中提取用户名用于测试
            String authHeader = request.getHeader("Authorization");
            String username = "test_user"; // 默认测试用户

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String tokenUsername = JWTUtils.getUsernameFromToken(token);
                if (tokenUsername != null && !tokenUsername.trim().isEmpty()) {
                    username = tokenUsername;
                }
            }

            // 测试同步调用
            ChatRequest testRequest = new ChatRequest("test", "你好，请简短回复", null);
            ChatResponse chatResponse = chatAgentService.chat(testRequest, username);

            result.put("status", "success");
            result.put("message", "AI服务正常");
            result.put("response", chatResponse.getAiMessage() != null ? chatResponse.getAiMessage().getContent() : "无响应");

            return Result.success(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "AI服务异常");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());

            return Result.error(500, "AI测试失败: " + e.getMessage());
        }
    }

    /**
     * 保存上传文件（如果有的话）
     */
    private List<Attachment> saveFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return null;
        return fileStorageService.saveFiles(files);
    }
}