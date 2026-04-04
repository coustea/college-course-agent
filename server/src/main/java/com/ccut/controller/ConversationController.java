package com.ccut.controller;

import com.ccut.dto.CreateConversationRequest;
import com.ccut.dto.Result;
import com.ccut.entity.Conversation;
import com.ccut.entity.Message;
import com.ccut.service.ConversationService;
import com.ccut.service.MessageService;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理 Controller
 */
@RestController
@RequestMapping("/api/ai/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Value("${jwt.enabled:true}")
    private boolean jwtEnabled;

    /**
     * 从请求中获取用户名（支持 JWT 开关）
     */
    private String getUsernameFromRequest(HttpServletRequest httpRequest, String pathUsername) {
        if (jwtEnabled) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return JWTUtils.getUsernameFromToken(token);
            }
            throw new RuntimeException("未授权访问");
        } else {
            return pathUsername;
        }
    }

    /**
     * 从请求中获取用户名（用于 POST 请求）
     */
    private String getUsernameFromRequest(HttpServletRequest httpRequest) {
        if (jwtEnabled) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return JWTUtils.getUsernameFromToken(token);
            }
            throw new RuntimeException("未授权访问");
        } else {
            return "dev_user";
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createConversation(
            @RequestBody CreateConversationRequest request,
            HttpServletRequest httpRequest) {
        // 从 JWT token 中提取当前登录用户的用户名
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未授权访问");
        }
        String token = authHeader.substring(7);
        String username = JWTUtils.getUsernameFromToken(token);

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("无法获取用户信息");
        }

        // 使用 token 中的用户名创建会话
        Conversation conversation = conversationService.createConversation(
                username,
                request.title()
        );

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversation.getConversationId());
        data.put("title", conversation.getTitle());
        data.put("sequenceNum", conversation.getSequenceNum());
        data.put("createdAt", conversation.getCreatedAt());

        return Result.success(data);
    }

    /**
     * 获取用户所有会话
     */
    @GetMapping("/list/{username}")
    public Result<List<Conversation>> getUserConversations(
            @PathVariable String username,
            HttpServletRequest httpRequest) {
        // 获取用户名（支持 JWT 开关）
        String actualUsername = getUsernameFromRequest(httpRequest, username);

        if (actualUsername == null || actualUsername.trim().isEmpty()) {
            throw new RuntimeException("无法获取用户信息");
        }

        // 如果 JWT 启用，验证路径参数是否与 token 一致
        if (jwtEnabled && !actualUsername.equals(username)) {
            throw new RuntimeException("无权访问其他用户的会话");
        }

        // 返回用户会话
        List<Conversation> conversations = conversationService.getUserConversations(actualUsername);
        return Result.success(conversations);
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/{conversationId}/messages")
    public Result<Map<String, Object>> getConversationMessages(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        // 从 JWT token 中提取当前登录用户的用户名
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未授权访问");
        }
        String token = authHeader.substring(7);
        String username = JWTUtils.getUsernameFromToken(token);

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("无法获取用户信息");
        }

        // 验证会话是否存在
        Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        // 验证会话所有者是否为当前登录用户
        if (!conversation.getUsername().equals(username)) {
            throw new RuntimeException("无权访问其他用户的会话");
        }

        // 加载消息历史
        List<Message> messages = messageService.loadConversationHistory(conversationId, conversation.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("title", conversation.getTitle());
        data.put("messages", messages);

        return Result.success(data);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{conversationId}")
    public Result<String> deleteConversation(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        // 从 JWT token 中提取当前登录用户的用户名
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未授权访问");
        }
        String token = authHeader.substring(7);
        String username = JWTUtils.getUsernameFromToken(token);

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("无法获取用户信息");
        }

        // 验证会话是否存在
        Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        // 验证会话所有者是否为当前登录用户
        if (!conversation.getUsername().equals(username)) {
            throw new RuntimeException("无权删除其他用户的会话");
        }

        // 删除会话（会级联删除相关消息）
        conversationService.deleteConversation(conversationId);

        return Result.success("删除会话成功");
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{conversationId}/title")
    public Result<String> updateConversationTitle(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String newTitle = request.get("title");

        // 从 JWT token 中提取当前登录用户的用户名
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未授权访问");
        }
        String token = authHeader.substring(7);
        String username = JWTUtils.getUsernameFromToken(token);

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("无法获取用户信息");
        }

        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }

        // 验证会话是否存在
        Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        // 验证会话所有者是否为当前登录用户
        if (!conversation.getUsername().equals(username)) {
            throw new RuntimeException("无权修改其他用户的会话");
        }

        // 更新标题
        conversationService.updateConversationTitle(conversationId, newTitle);

        return Result.success("更新标题成功");
    }
}
