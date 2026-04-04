package com.ccut.controller;

import com.ccut.context.UserContext;
import com.ccut.dto.Attachment;
import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import com.ccut.dto.Result;
import com.ccut.entity.Message;
import com.ccut.entity.User;
import com.ccut.service.ChatAgentService;
import com.ccut.service.FileStorageService;
import com.ccut.service.MessageService;
import com.ccut.service.UserService;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
public class ChatController {

    @Autowired
    private ChatAgentService chatAgentService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserService userService;

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ChatResponse> chat(
            @RequestPart("conversationId") String conversationId,
            @RequestPart("message") String message,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        User user = resolveUser(request);
        if (user == null) {
            throw new IllegalArgumentException("无法获取用户信息");
        }

        try {
            List<Attachment> attachments = saveFiles(files);
            ChatRequest chatRequest = new ChatRequest(conversationId, message, attachments);
            ChatResponse chatResponse = chatAgentService.chat(chatRequest, user.getUsername(), user.getRole().name());
            return Result.success(chatResponse);
        } finally {
            UserContext.clear();
        }
    }

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
            User user = resolveUser(request);
            if (user == null) {
                return Flux.just("{\"code\": 400, \"message\": \"无法获取用户信息\"}");
            }

            try {
                List<Attachment> attachments = saveFiles(files);
                ChatRequest chatRequest = new ChatRequest(conversationId, message, attachments);

                return chatAgentService.chatStream(chatRequest, user.getUsername(), user.getRole().name())
                        .onErrorResume(e -> {
                            log.error("流式响应错误：username={}, conversationId={}, 错误：{}",
                                    user.getUsername(), conversationId, e.getMessage());
                            return Flux.just("{\"code\": 500, \"message\": \"" + e.getMessage() + "\"}");
                        });
            } finally {
                UserContext.clear();
            }
        } catch (Exception e) {
            log.error("AI 流式聊天请求处理失败：conversationId={}, 错误：{}", conversationId, e.getMessage());
            return Flux.just("{\"code\": 500, \"message\": \"聊天失败：" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/history")
    public Result<List<Message>> getChatHistory(HttpServletRequest request) {
        String username = JWTUtils.resolveUsername(request);
        List<Message> messages = messageService.findByUsername(username);
        return Result.success(messages);
    }

    @DeleteMapping("/delete")
    public Result<String> deleteChat(HttpServletRequest request) {
        String username = JWTUtils.resolveUsername(request);
        List<Message> messages = messageService.findByUsername(username);
        if (!messages.isEmpty()) {
            String conversationId = messages.get(0).getConversationId();
            messageService.deleteMessages(conversationId, username);
        }
        return Result.success("聊天记录删除成功");
    }

    @GetMapping("/test")
    public Result<Map<String, Object>> testAIConnection(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String username = "test_user";
        String role = "student";

        try {
            username = JWTUtils.resolveUsername(request);
        } catch (IllegalArgumentException ignored) {
            // 无 token 时使用默认测试用户
        }

        User user = userService.getByUsername(username);
        if (user != null) {
            role = user.getRole().name();
            UserContext.set(user.getId(), username, user.getRole());
        }

        try {
            ChatRequest testRequest = new ChatRequest("test", "你好，请简短回复", null);
            ChatResponse chatResponse = chatAgentService.chat(testRequest, username, role);

            result.put("status", "success");
            result.put("message", "AI 服务正常");
            result.put("response", chatResponse.getAiMessage() != null ? chatResponse.getAiMessage().getContent() : "无响应");
            result.put("username", username);

            return Result.success(result);
        } finally {
            UserContext.clear();
        }
    }

    // ======================== 私有方法 ========================

    private User resolveUser(HttpServletRequest request) {
        String username;
        try {
            username = JWTUtils.resolveUsername(request);
        } catch (Exception e) {
            log.warn("AI 聊天未授权访问：{}", e.getMessage());
            return null;
        }

        User user = userService.getByUsername(username);
        if (user == null) {
            log.warn("AI 聊天用户不存在：username={}", username);
            return null;
        }

        UserContext.set(user.getId(), user.getUsername(), user.getRole());
        return user;
    }

    private List<Attachment> saveFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        return fileStorageService.saveFiles(files);
    }
}
