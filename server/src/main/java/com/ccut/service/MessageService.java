package com.ccut.service;

import com.ccut.entity.Message;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 保存用户消息（同步写MySQL + 异步更新Redis）
     * @param conversationId 会话ID
     * @param content 消息内容
     * @param username 用户名（用于Redis key）
     * @return 保存的消息对象
     */
    Message saveUserMessage(String conversationId, String content, String username);

    /**
     * 保存AI消息（同步写MySQL + 异步更新Redis）
     * @param conversationId 会话ID
     * @param content 消息内容
     * @param username 用户名（用于Redis key）
     * @return 保存的消息对象
     */
    Message saveAIMessage(String conversationId, String content, String username);

    /**
     * 从缓存加载会话历史（如果缓存没有，从MySQL加载）
     * @param conversationId 会话ID
     * @param username 用户名（用于Redis key）
     * @return 消息列表（按序号升序）
     */
    List<Message> loadConversationHistory(String conversationId, String username);

    /**
     * 根据用户名查询所有消息（直接从MySQL加载）
     * @param username 用户名
     * @return 消息列表（按序号升序）
     */
    List<Message> findByUsername(String username);

    /**
     * 刷新会话缓存（从MySQL重新加载到Redis）
     * @param conversationId 会话ID
     * @param username 用户名（用于Redis key）
     */
    void refreshConversationCache(String conversationId, String username);

    /**
     * 获取会话的消息数量
     * @param conversationId 会话ID
     * @return 消息数量
     */
    int getMessageCount(String conversationId);

    /**
     * 删除会话的所有消息
     * @param conversationId 会话ID
     * @param username 用户名（用于Redis key）
     */
    void deleteMessages(String conversationId, String username);
}
