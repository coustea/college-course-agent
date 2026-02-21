package com.ccut.controller;

import com.ccut.dto.CreateConversationRequest;
import com.ccut.dto.Result;
import com.ccut.entity.Conversation;
import com.ccut.entity.Message;
import com.ccut.service.ConversationService;
import com.ccut.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 创建新会话
     * POST /api/ai/conversation/create
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createConversation(@RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.createConversation(
                request.username(),
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
     * GET /api/ai/conversations/{username}
     */
    @GetMapping("/list/{username}")
    public Result<List<Conversation>> getUserConversations(@PathVariable String username) {
        List<Conversation> conversations = conversationService.getUserConversations(username);
        return Result.success(conversations);
    }

    /**
     * 获取会话历史消息
     * GET /api/ai/conversation/{conversationId}/messages
     */
    @GetMapping("/{conversationId}/messages")
    public Result<Map<String, Object>> getConversationMessages(@PathVariable String conversationId) {
        // 验证会话是否存在
        Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return Result.error(404, "会话不存在");
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
     * DELETE /api/ai/conversation/{conversationId}
     */
    @DeleteMapping("/{conversationId}")
    public Result<String> deleteConversation(@PathVariable String conversationId) {
        // 验证会话是否存在
        Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return Result.error(404, "会话不存在");
        }

        // 删除会话（会级联删除相关消息）
        conversationService.deleteConversation(conversationId);

        return Result.success("删除会话成功");
    }

    /**
     * 更新会话标题
     * PUT /api/ai/conversation/{conversationId}/title
     */
    @PutMapping("/{conversationId}/title")
    public Result<String> updateConversationTitle(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request) {

        String newTitle = request.get("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return Result.error(400, "标题不能为空");
        }

        // 验证会话是否存在
        Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return Result.error(404, "会话不存在");
        }

        // 更新标题
        conversationService.updateConversationTitle(conversationId, newTitle);

        return Result.success("更新标题成功");
    }
}
