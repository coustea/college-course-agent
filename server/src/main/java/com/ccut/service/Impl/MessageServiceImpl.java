package com.ccut.service.Impl;

import com.ccut.entity.Message;
import com.ccut.mapper.MessageMapper;
import com.ccut.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息服务实现
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "chat:messages:"; // Redis key前缀
    private static final long CACHE_TTL_HOURS = 24; // 缓存过期时间：24小时

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Message saveUserMessage(String conversationId, String content) {
        // 获取该会话的下一个消息序号
        int nextSequenceNum = messageMapper.getNextSequenceNum(conversationId);

        // 创建用户消息对象
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("user"); // 用户消息角色
        message.setContent(content);
        message.setSequenceNum(nextSequenceNum);
        message.setCreatedAt(LocalDateTime.now());
        message.setTokensUsed(0);

        // 同步保存到MySQL（确保数据持久化）
        messageMapper.insert(message);

        // 异步更新Redis缓存（不阻塞主流程）
        addToCacheAsync(message);

        return message;
    }

    @Override
    public Message saveAIMessage(String conversationId, String content) {
        // 获取该会话的下一个消息序号
        int nextSequenceNum = messageMapper.getNextSequenceNum(conversationId);

        // 创建AI回复消息对象
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("assistant"); // AI助手角色
        message.setContent(content);
        message.setSequenceNum(nextSequenceNum);
        message.setCreatedAt(LocalDateTime.now());
        message.setTokensUsed(0);

        // 同步保存到MySQL（确保数据持久化）
        messageMapper.insert(message);

        // 异步更新Redis缓存（不阻塞主流程）
        addToCacheAsync(message);

        return message;
    }

    @Override
    public List<Message> loadConversationHistory(String conversationId) {
        String redisKey = REDIS_KEY_PREFIX + conversationId;

        try {
            // 先从Redis缓存读取（快速）
            List<Object> cachedMessages = redisTemplate.opsForList().range(redisKey, 0, -1);

            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                logger.debug("Redis cache hit for conversation: {}", conversationId);
                // 缓存命中，直接返回
                return cachedMessages.stream()
                        .map(obj -> (Message) obj)
                        .toList();
            }
        } catch (Exception e) {
            logger.warn("Failed to load from Redis for conversation: {}, error: {}", conversationId, e.getMessage());
        }

        // 缓存未命中，从MySQL加载（回源）
        logger.debug("Redis cache miss for conversation: {}, loading from MySQL", conversationId);
        List<Message> messagesFromDB = messageMapper.findByConversationId(conversationId);

        // 异步写入Redis（缓存回填，不阻塞主流程）
        if (!messagesFromDB.isEmpty()) {
            refreshCacheAsync(conversationId, messagesFromDB);
        }

        return messagesFromDB;
    }

    @Override
    @Async("cacheExecutor")
    public void refreshConversationCache(String conversationId) {
        // 从MySQL重新加载完整消息列表
        List<Message> allMessages = messageMapper.findByConversationId(conversationId);

        // 刷新缓存
        refreshCacheInternal(conversationId, allMessages);
    }

    @Override
    public int getMessageCount(String conversationId) {
        return messageMapper.countByConversationId(conversationId);
    }

    @Override
    public void deleteMessages(String conversationId) {
        // 删除数据库中的消息
        messageMapper.deleteByConversationId(conversationId);

        // 异步删除Redis缓存
        deleteCacheAsync(conversationId);
    }

    /**
     * 异步添加消息到缓存
     */
    @Async("cacheExecutor")
    protected void addToCacheAsync(Message message) {
        try {
            String redisKey = REDIS_KEY_PREFIX + message.getConversationId();
            redisTemplate.opsForList().rightPush(redisKey, message);
            // 设置过期时间
            redisTemplate.expire(redisKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
            logger.debug("Added message to cache: conversationId={}, sequenceNum={}",
                    message.getConversationId(), message.getSequenceNum());
        } catch (Exception e) {
            logger.error("Failed to add message to Redis cache: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步刷新缓存
     */
    @Async("cacheExecutor")
    protected void refreshCacheAsync(String conversationId, List<Message> messages) {
        refreshCacheInternal(conversationId, messages);
    }

    /**
     * 内部刷新缓存方法
     */
    private void refreshCacheInternal(String conversationId, List<Message> messages) {
        try {
            String redisKey = REDIS_KEY_PREFIX + conversationId;

            // 清空旧缓存
            redisTemplate.delete(redisKey);

            // 写入新缓存
            if (!messages.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(redisKey, messages.toArray());
                redisTemplate.expire(redisKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
                logger.debug("Refreshed cache for conversation: {}, message count: {}",
                        conversationId, messages.size());
            }
        } catch (Exception e) {
            logger.error("Failed to refresh cache for conversation: {}, error: {}",
                    conversationId, e.getMessage(), e);
        }
    }

    /**
     * 异步删除缓存
     */
    @Async("cacheExecutor")
    protected void deleteCacheAsync(String conversationId) {
        try {
            String redisKey = REDIS_KEY_PREFIX + conversationId;
            redisTemplate.delete(redisKey);
            logger.debug("Deleted cache for conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Failed to delete cache for conversation: {}, error: {}",
                    conversationId, e.getMessage(), e);
        }
    }
}
