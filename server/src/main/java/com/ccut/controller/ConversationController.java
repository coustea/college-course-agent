package com.ccut.controller;

import com.ccut.dto.CreateConversationRequest;
import com.ccut.dto.Result;
import com.ccut.entity.Conversation;
import com.ccut.entity.Message;
import com.ccut.service.ConversationService;
import com.ccut.service.MessageService;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理 Controller
 */
@Slf4j
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
        log.debug("收到创建会话请求：URI=/api/ai/conversation/create, 参数：title={}", request.title());
        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("创建会话未授权访问");
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("创建会话无法获取用户信息");
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行创建会话业务：username={}, title={}", username, request.title());

            // 使用 token 中的用户名创建会话
            Conversation conversation = conversationService.createConversation(
                    username,
                    request.title()
            );

            log.info("创建会话成功：username={}, conversationId={}, title={}", username, conversation.getConversationId(), conversation.getTitle());

            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversation.getConversationId());
            data.put("title", conversation.getTitle());
            data.put("sequenceNum", conversation.getSequenceNum());
            data.put("createdAt", conversation.getCreatedAt());

            return Result.success(data);
        } catch (Exception e) {
            log.error("创建会话失败：title={}, 错误：{}", request.title(), e.getMessage(), e);
            return Result.error(500, "创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户所有会话
     */
    @GetMapping("/list/{username}")
    public Result<List<Conversation>> getUserConversations(
            @PathVariable String username,
            HttpServletRequest httpRequest) {
        log.debug("收到获取用户会话列表请求：URI=/api/ai/conversation/list/{}, 参数：username={}", username, username);
        try {
            // 获取用户名（支持 JWT 开关）
            String actualUsername = getUsernameFromRequest(httpRequest, username);

            if (actualUsername == null || actualUsername.trim().isEmpty()) {
                log.warn("获取用户会话列表无法获取用户信息");
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行获取用户会话列表业务：pathUsername={}, actualUsername={}", username, actualUsername);

            // 如果 JWT 启用，验证路径参数是否与 token 一致
            if (jwtEnabled && !actualUsername.equals(username)) {
                log.error("用户名不匹配：pathUsername={}, actualUsername={}", username, actualUsername);
                return Result.error(403, "无权访问其他用户的会话");
            }

            // 返回用户会话
            List<Conversation> conversations = conversationService.getUserConversations(actualUsername);
            log.info("获取用户会话列表成功：username={}, 会话数={}", actualUsername, conversations.size());
            return Result.success(conversations);
        } catch (Exception e) {
            log.error("获取会话列表失败：username={}, 错误：{}", username, e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "获取会话列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/{conversationId}/messages")
    public Result<Map<String, Object>> getConversationMessages(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        log.debug("收到获取会话消息请求：URI=/api/ai/conversation/{}/messages, 参数：conversationId={}", conversationId, conversationId);
        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("获取会话消息未授权访问：conversationId={}", conversationId);
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("获取会话消息无法获取用户信息：conversationId={}", conversationId);
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行获取会话消息业务：username={}, conversationId={}", username, conversationId);

            // 验证会话是否存在
            Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                log.warn("会话不存在：conversationId={}", conversationId);
                return Result.error(404, "会话不存在");
            }

            // 验证会话所有者是否为当前登录用户
            if (!conversation.getUsername().equals(username)) {
                log.error("无权访问其他用户的会话：username={}, conversationOwner={}", username, conversation.getUsername());
                return Result.error(403, "无权访问其他用户的会话");
            }

            // 加载消息历史
            List<Message> messages = messageService.loadConversationHistory(conversationId, conversation.getUsername());
            log.info("获取会话消息成功：conversationId={}, username={}, 消息数={}", conversationId, username, messages.size());

            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversationId);
            data.put("title", conversation.getTitle());
            data.put("messages", messages);

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取会话消息失败：conversationId={}, 错误：{}", conversationId, e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "获取会话消息失败：" + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{conversationId}")
    public Result<String> deleteConversation(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        log.debug("收到删除会话请求：URI=/api/ai/conversation/{}, 参数：conversationId={}", conversationId, conversationId);
        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("删除会话未授权访问：conversationId={}", conversationId);
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("删除会话无法获取用户信息：conversationId={}", conversationId);
                return Result.error(401, "无法获取用户信息");
            }

            log.info("执行删除会话业务：username={}, conversationId={}", username, conversationId);

            // 验证会话是否存在
            Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                log.warn("会话不存在：conversationId={}", conversationId);
                return Result.error(404, "会话不存在");
            }

            // 验证会话所有者是否为当前登录用户
            if (!conversation.getUsername().equals(username)) {
                log.error("无权删除其他用户的会话：username={}, conversationOwner={}", username, conversation.getUsername());
                return Result.error(403, "无权删除其他用户的会话");
            }

            // 删除会话（会级联删除相关消息）
            conversationService.deleteConversation(conversationId);
            log.info("删除会话成功：conversationId={}, username={}", conversationId, username);

            return Result.success("删除会话成功");
        } catch (Exception e) {
            log.error("删除会话失败：conversationId={}, 错误：{}", conversationId, e.getMessage(), e);
            return Result.error(500, "删除会话失败：" + e.getMessage());
        }
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
        log.debug("收到更新会话标题请求：URI=/api/ai/conversation/{}/title, 参数：conversationId={}, title={}", conversationId, newTitle);

        try {
            // 从 JWT token 中提取当前登录用户的用户名
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("更新会话标题未授权访问：conversationId={}", conversationId);
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("更新会话标题无法获取用户信息：conversationId={}", conversationId);
                return Result.error(401, "无法获取用户信息");
            }

            if (newTitle == null || newTitle.trim().isEmpty()) {
                log.warn("更新会话标题参数错误：title 为空");
                return Result.error(400, "标题不能为空");
            }

            log.info("执行更新会话标题业务：username={}, conversationId={}, title={}", username, conversationId, newTitle);

            // 验证会话是否存在
            Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                log.warn("会话不存在：conversationId={}", conversationId);
                return Result.error(404, "会话不存在");
            }

            // 验证会话所有者是否为当前登录用户
            if (!conversation.getUsername().equals(username)) {
                log.error("无权修改其他用户的会话：username={}, conversationOwner={}", username, conversation.getUsername());
                return Result.error(403, "无权修改其他用户的会话");
            }

            // 更新标题
            conversationService.updateConversationTitle(conversationId, newTitle);
            log.info("更新会话标题成功：conversationId={}, username={}, title={}", conversationId, username, newTitle);

            return Result.success("更新标题成功");
        } catch (Exception e) {
            log.error("更新标题失败：conversationId={}, 错误：{}", conversationId, e.getMessage(), e);
            return Result.error(500, "更新标题失败：" + e.getMessage());
        }
    }
}
