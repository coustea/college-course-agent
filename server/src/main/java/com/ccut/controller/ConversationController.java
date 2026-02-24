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
     * 如果 JWT 启用：从 token 中解析
     * 如果 JWT 禁用：从请求参数/路径/请求体中获取
     */
    private String getUsernameFromRequest(HttpServletRequest httpRequest, String pathUsername) {
        if (jwtEnabled) {
            // JWT 启用：从 token 中解析
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return JWTUtils.getUsernameFromToken(token);
            }
            throw new RuntimeException("未授权访问");
        } else {
            // JWT 禁用：使用路径参数中的用户名
            return pathUsername;
        }
    }

    /**
     * 从请求中获取用户名（用于 POST 请求）
     */
    private String getUsernameFromRequest(HttpServletRequest httpRequest) {
        if (jwtEnabled) {
            // JWT 启用：从 token 中解析
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return JWTUtils.getUsernameFromToken(token);
            }
            throw new RuntimeException("未授权访问");
        } else {
            // JWT 禁用：返回默认用户（开发环境）
            return "dev_user";
        }
    }

    /**
     * 创建新会话
     * POST /api/ai/conversation/create
     * 从JWT token中获取当前登录用户的用户名
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createConversation(
            @RequestBody CreateConversationRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            System.out.println("========== [Create Conversation] ==========");
            System.out.println("Username from JWT: " + username);
            System.out.println("Title: " + request.title());

            // 使用token中的用户名创建会话
            Conversation conversation = conversationService.createConversation(
                    username,
                    request.title()
            );

            System.out.println("Created conversationId: " + conversation.getConversationId());

            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversation.getConversationId());
            data.put("title", conversation.getTitle());
            data.put("sequenceNum", conversation.getSequenceNum());
            data.put("createdAt", conversation.getCreatedAt());

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户所有会话
     * GET /api/ai/conversation/list
     * 从JWT token或路径参数中获取用户名
     */
    @GetMapping("/list/{username}")
    public Result<List<Conversation>> getUserConversations(
            @PathVariable String username,
            HttpServletRequest httpRequest) {
        try {
            // 获取用户名（支持 JWT 开关）
            String actualUsername = getUsernameFromRequest(httpRequest, username);

            if (actualUsername == null || actualUsername.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            System.out.println("========== [Get User Conversations] ==========");
            System.out.println("Path username: " + username);
            System.out.println("Actual username: " + actualUsername);

            // 如果 JWT 启用，验证路径参数是否与 token 一致
            if (jwtEnabled && !actualUsername.equals(username)) {
                System.out.println("ERROR: Username mismatch!");
                return Result.error(403, "无权访问其他用户的会话");
            }

            // 返回用户会话
            List<Conversation> conversations = conversationService.getUserConversations(actualUsername);
            System.out.println("Found " + conversations.size() + " conversations for user: " + actualUsername);
            if (!conversations.isEmpty()) {
                conversations.forEach(conv -> System.out.println("  - " + conv.getConversationId()));
            }

            return Result.success(conversations);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话历史消息
     * GET /api/ai/conversation/{conversationId}/messages
     * 验证会话所有者是否为当前登录用户
     */
    @GetMapping("/{conversationId}/messages")
    public Result<Map<String, Object>> getConversationMessages(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            System.out.println("========== [Get Conversation Messages] ==========");
            System.out.println("ConversationId: " + conversationId);
            System.out.println("Token username: " + username);

            // 验证会话是否存在
            Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                System.out.println("ERROR: Conversation not found!");
                return Result.error(404, "会话不存在");
            }

            System.out.println("Conversation owner: " + conversation.getUsername());

            // 验证会话所有者是否为当前登录用户
            if (!conversation.getUsername().equals(username)) {
                System.out.println("ERROR: User does not own this conversation!");
                return Result.error(403, "无权访问其他用户的会话");
            }

            // 加载消息历史
            List<Message> messages = messageService.loadConversationHistory(conversationId, conversation.getUsername());
            System.out.println("Loaded " + messages.size() + " messages");

            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversationId);
            data.put("title", conversation.getTitle());
            data.put("messages", messages);

            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取会话消息失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     * DELETE /api/ai/conversation/{conversationId}
     * 验证会话所有者是否为当前登录用户
     */
    @DeleteMapping("/{conversationId}")
    public Result<String> deleteConversation(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            // 验证会话是否存在
            Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                return Result.error(404, "会话不存在");
            }

            // 验证会话所有者是否为当前登录用户
            if (!conversation.getUsername().equals(username)) {
                return Result.error(403, "无权删除其他用户的会话");
            }

            // 删除会话（会级联删除相关消息）
            conversationService.deleteConversation(conversationId);

            return Result.success("删除会话成功");
        } catch (Exception e) {
            return Result.error(500, "删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 更新会话标题
     * PUT /api/ai/conversation/{conversationId}/title
     * 验证会话所有者是否为当前登录用户
     */
    @PutMapping("/{conversationId}/title")
    public Result<String> updateConversationTitle(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        try {
            // 从JWT token中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "无法获取用户信息");
            }

            String newTitle = request.get("title");
            if (newTitle == null || newTitle.trim().isEmpty()) {
                return Result.error(400, "标题不能为空");
            }

            // 验证会话是否存在
            Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                return Result.error(404, "会话不存在");
            }

            // 验证会话所有者是否为当前登录用户
            if (!conversation.getUsername().equals(username)) {
                return Result.error(403, "无权修改其他用户的会话");
            }

            // 更新标题
            conversationService.updateConversationTitle(conversationId, newTitle);

            return Result.success("更新标题成功");
        } catch (Exception e) {
            return Result.error(500, "更新标题失败: " + e.getMessage());
        }
    }
}
