package com.ccut.service;

import com.ccut.entity.Conversation;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ConversationService {

    /**
     * 创建新会话
     * @param username 用户名
     * @param title 会话标题（可选，默认"新对话"）
     * @return 创建的会话对象
     */
    Conversation createConversation(String username, String title);

    /**
     * 获取用户的所有会话
     * @param username 用户名
     * @return 会话列表（按更新时间倒序）
     */
    List<Conversation> getUserConversations(String username);

    /**
     * 根据会话ID获取会话
     * @param conversationId 会话ID
     * @return 会话对象
     */
    Conversation getConversation(String conversationId);

    /**
     * 删除会话
     * @param conversationId 会话ID
     */
    void deleteConversation(String conversationId);

    /**
     * 更新会话标题
     * @param conversationId 会话ID
     * @param title 新标题
     */
    void updateConversationTitle(String conversationId, String title);

    /**
     * 生成会话ID（格式：username:序号）
     * @param username 用户名
     * @return 会话ID
     */
    String generateConversationId(String username);
}
