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
     * @return 保存的消息对象
     */
    Message saveUserMessage(String conversationId, String content);

    /**
     * 保存AI消息（同步写MySQL + 异步更新Redis）
     * @param conversationId 会话ID
     * @param content 消息内容
     * @return 保存的消息对象
     */
    Message saveAIMessage(String conversationId, String content);

    /**
     * 从缓存加载会话历史（如果缓存没有，从MySQL加载）
     * @param conversationId 会话ID
     * @return 消息列表（按序号升序）
     */
    List<Message> loadConversationHistory(String conversationId);

    /**
     * 刷新会话缓存（从MySQL重新加载到Redis）
     * @param conversationId 会话ID
     */
    void refreshConversationCache(String conversationId);

    /**
     * 获取会话的消息数量
     * @param conversationId 会话ID
     * @return 消息数量
     */
    int getMessageCount(String conversationId);

    /**
     * 删除会话的所有消息
     * @param conversationId 会话ID
     */
    void deleteMessages(String conversationId);
}
