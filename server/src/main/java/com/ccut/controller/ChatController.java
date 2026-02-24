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

    /**
     * 同步聊天（支持可选文件上传）
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ChatResponse> chat(
            @RequestPart("conversationId") String conversationId,
            @RequestPart("message") String message,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request) {
        log.debug("收到 AI 聊天请求 (同步)：URI=/api/ai/chat/send, 参数：conversationId={}, message 长度={}, 文件数={}",
                conversationId, message != null ? message.length() : 0, files != null ? files.size() : 0);

        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("AI 聊天参数错误：conversationId 为空");
            return Result.error(400, "conversationId 不能为空");
        }
        if (message == null || message.trim().isEmpty()) {
            log.warn("AI 聊天参数错误：message 为空");
            return Result.error(400, "消息内容不能为空");
        }

        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("AI 聊天未授权访问：conversationId={}", conversationId);
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("AI 聊天无法获取用户信息：conversationId={}", conversationId);
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行 AI 聊天业务 (同步)：username={}, conversationId={}, message 长度={}", username, conversationId, message.length());

            List<Attachment> attachments = saveFiles(files);
            if (attachments != null && !attachments.isEmpty()) {
                log.debug("保存附件：count={}", attachments.size());
            }

            ChatRequest chatRequest = new ChatRequest(conversationId, message, attachments);
            ChatResponse chatResponse = chatAgentService.chat(chatRequest, username);
            log.debug("AI 聊天成功 (同步)：username={}, conversationId={}, response 长度={}",
                    username, conversationId, chatResponse.getAiMessage() != null ? chatResponse.getAiMessage().getContent().length() : 0);
            return Result.success(chatResponse);
        } catch (Exception e) {
            log.error("AI 聊天失败 (同步)：conversationId={}, 错误：{}", conversationId, e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "聊天失败：" + e.getMessage());
        }
    }

    /**
     * 流式聊天（SSE，支持可选文件上传）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<String> chatStream(
            @RequestPart("conversationId") String conversationId,
            @RequestPart("message") String message,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest request) {
        log.debug("收到 AI 聊天请求 (流式)：URI=/api/ai/chat/stream, 参数：conversationId={}, message 长度={}, 文件数={}",
                conversationId, message != null ? message.length() : 0, files != null ? files.size() : 0);

        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("AI 流式聊天参数错误：conversationId 为空");
            return Flux.just("{\"code\": 400, \"message\": \"conversationId 不能为空\"}");
        }
        if (message == null || message.trim().isEmpty()) {
            log.warn("AI 流式聊天参数错误：message 为空");
            return Flux.just("{\"code\": 400, \"message\": \"消息内容不能为空\"}");
        }

        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("AI 流式聊天未授权访问：conversationId={}", conversationId);
                return Flux.just("{\"code\": 401, \"message\": \"未授权访问\"}");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("AI 流式聊天无法获取用户信息：conversationId={}", conversationId);
                return Flux.just("{\"code\": 401, \"message\": \"无法获取用户信息\"}");
            }

            log.info("执行 AI 聊天业务 (流式)：username={}, conversationId={}, message 长度={}", username, conversationId, message.length());

            List<Attachment> attachments = saveFiles(files);
            if (attachments != null && !attachments.isEmpty()) {
                log.debug("保存附件：count={}", attachments.size());
            }

            ChatRequest chatRequest = new ChatRequest(conversationId, message, attachments);
            log.info("开始流式响应：username={}, conversationId={}", username, conversationId);
            return chatAgentService.chatStream(chatRequest, username)
                    .doOnNext(chunk -> {
                        log.debug("发送 SSE chunk: 长度={}", chunk.length());
                    })
                    .onErrorResume(e -> {
                        log.error("流式响应错误：username={}, conversationId={}, 错误：{}", username, conversationId, e.getMessage());
                        return Flux.just("{\"code\": 500, \"message\": \"" + e.getMessage() + "\"}");
                    })
                    .doOnComplete(() -> {
                        log.info("SSE 流发送完成：username={}, conversationId={}", username, conversationId);
                    });
        } catch (Exception e) {
            log.error("AI 流式聊天请求处理失败：conversationId={}, 错误：{}", conversationId, e.getMessage());
            return Flux.just("{\"code\": 500, \"message\": \"聊天失败：" + e.getMessage() + "\"}");
        }
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/history")
    public Result<List<Message>> getChatHistory(HttpServletRequest request) {
        log.debug("收到获取聊天记录请求：URI=/api/ai/chat/history");
        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("获取聊天记录未授权访问");
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("获取聊天记录无法获取用户信息");
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行获取聊天记录业务：username={}", username);
            // 只查询当前登录用户的消息
            List<Message> messages = messageService.findByUsername(username);
            log.debug("获取聊天记录成功：username={}, 消息数={}", username, messages.size());
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取聊天记录失败：错误：{}", e.getMessage(), e);
            return Result.error(500, "获取聊天记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除聊天记录
     */
    @DeleteMapping("/delete")
    public Result<String> deleteChat(HttpServletRequest request) {
        log.debug("收到删除聊天记录请求：URI=/api/ai/chat/delete");
        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("删除聊天记录未授权访问");
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("删除聊天记录无法获取用户信息");
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行删除聊天记录业务：username={}", username);
            // 只删除当前登录用户的消息
            List<Message> messages = messageService.findByUsername(username);
            if (!messages.isEmpty()) {
                String conversationId = messages.get(0).getConversationId();
                log.debug("删除聊天记录：username={}, conversationId={}, 消息数={}", username, conversationId, messages.size());
                messageService.deleteMessages(conversationId, username);
            }
            log.info("删除聊天记录成功：username={}", username);
            return Result.success("聊天记录删除成功");
        } catch (Exception e) {
            log.error("删除聊天记录失败：错误：{}", e.getMessage(), e);
            return Result.error(500, "删除聊天记录失败：" + e.getMessage());
        }
    }

    /**
     * 测试 AI 连接（用于调试）
     */
    @GetMapping("/test")
    public Result<Map<String, Object>> testAIConnection(HttpServletRequest request) {
        log.debug("收到测试 AI 连接请求：URI=/api/ai/chat/test");
        Map<String, Object> result = new HashMap<>();

        try {
            // 从 JWT token 中提取用户名用于测试
            String authHeader = request.getHeader("Authorization");
            String username = "test_user"; // 默认测试用户

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String tokenUsername = JWTUtils.getUsernameFromToken(token);
                if (tokenUsername != null && !tokenUsername.trim().isEmpty()) {
                    username = tokenUsername;
                }
            }

            log.info("执行测试 AI 连接业务：username={}", username);
            // 测试同步调用
            ChatRequest testRequest = new ChatRequest("test", "你好，请简短回复", null);
            ChatResponse chatResponse = chatAgentService.chat(testRequest, username);

            result.put("status", "success");
            result.put("message", "AI 服务正常");
            result.put("response", chatResponse.getAiMessage() != null ? chatResponse.getAiMessage().getContent() : "无响应");
            result.put("username", username);

            log.debug("测试 AI 连接成功：username={}", username);
            return Result.success(result);
        } catch (Exception e) {
            log.error("测试 AI 连接失败：错误：{}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "AI 服务异常");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());

            return Result.error(500, "AI 测试失败：" + e.getMessage());
        }
    }

    /**
     * 保存上传文件（如果有的话）
     */
    private List<Attachment> saveFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            log.debug("没有文件需要保存");
            return null;
        }
        log.debug("开始保存文件：count={}, totalSize={} bytes", files.size(),
                files.stream().mapToLong(MultipartFile::getSize).sum());
        List<Attachment> attachments = fileStorageService.saveFiles(files);
        log.debug("文件保存完成：count={}", attachments != null ? attachments.size() : 0);
        return attachments;
    }
}
