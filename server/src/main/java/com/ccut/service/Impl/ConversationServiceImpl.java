package com.ccut.service.Impl;

import com.ccut.entity.Conversation;
import com.ccut.mapper.ConversationMapper;
import com.ccut.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务实现
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    public Conversation createConversation(String username, String title) {
        // 获取该用户的下一个会话序号（自增）
        int nextSequenceNum = conversationMapper.getNextSequenceNum(username);

        // 生成会话业务ID（格式：username:序号）
        String conversationId = generateConversationId(username);

        // 如果标题为空，使用默认标题
        if (title == null || title.trim().isEmpty()) {
            title = "新对话";
        }

        // 创建会话对象
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId); // 业务唯一标识
        conversation.setUsername(username);
        conversation.setSequenceNum(nextSequenceNum);
        conversation.setTitle(title);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        conversationMapper.insert(conversation);

        return conversation;
    }

    @Override
    public List<Conversation> getUserConversations(String username) {
        return conversationMapper.findByUsername(username);
    }

    @Override
    public Conversation getConversation(String conversationId) {
        return conversationMapper.findByConversationId(conversationId);
    }

    @Override
    public void deleteConversation(String conversationId) {
        conversationMapper.deleteByConversationId(conversationId);
    }

    @Override
    public void updateConversationTitle(String conversationId, String title) {
        conversationMapper.updateTitle(conversationId, title);
    }

    @Override
    public String generateConversationId(String username) {
        // 每个用户只有一个会话，序号固定为1
        return username + ":1";
    }

    @Override
    public Conversation getOrCreateUserConversation(String username) {
        // 尝试获取用户的唯一会话（序号为1）
        String conversationId = username + ":1";
        Conversation existingConversation = conversationMapper.findByConversationId(conversationId);

        if (existingConversation != null) {
            return existingConversation;
        }

        // 会话不存在，创建新会话
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setUsername(username);
        conversation.setSequenceNum(1);
        conversation.setTitle("AI助手对话");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        conversationMapper.insert(conversation);
        return conversation;
    }
}
