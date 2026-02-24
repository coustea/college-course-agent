package com.ccut.service.Impl;

import com.ccut.entity.Conversation;
import com.ccut.mapper.ConversationMapper;
import com.ccut.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务实现
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);

    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    public Conversation createConversation(String username, String title) {
        log.debug("执行方法：createConversation, 参数：username={}, title={}", username, title);
        long startTime = System.currentTimeMillis();
        try {
            // 获取该用户的下一个会话序号（自增）
            int nextSequenceNum = conversationMapper.getNextSequenceNum(username);

            // 生成会话业务 ID（格式：username:序号）
            String conversationId = generateConversationId(username);

            // 如果标题为空，使用默认标题
            if (title == null || title.trim().isEmpty()) {
                title = "新对话";
                log.info("标题为空，使用默认标题：username={}", username);
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

            long duration = System.currentTimeMillis() - startTime;
            log.info("创建会话成功：conversationId={}, username={}, title={}, 耗时={}ms", 
                    conversationId, username, title, duration);
            log.debug("方法返回：result={}", conversation);
            return conversation;
        } catch (Exception e) {
            log.error("创建会话失败：username={}, title={}, error={}", username, title, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Conversation> getUserConversations(String username) {
        log.debug("执行方法：getUserConversations, 参数：username={}", username);
        long startTime = System.currentTimeMillis();
        try {
            List<Conversation> result = conversationMapper.findByUsername(username);
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询用户会话列表成功：username={}, count={}, 耗时={}ms", username, result != null ? result.size() : 0, duration);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询用户会话列表失败：username={}, error={}", username, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Conversation getConversation(String conversationId) {
        log.debug("执行方法：getConversation, 参数：conversationId={}", conversationId);
        long startTime = System.currentTimeMillis();
        try {
            Conversation result = conversationMapper.findByConversationId(conversationId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询会话成功：conversationId={}, 耗时={}ms", conversationId, duration);
            log.debug("方法返回：result={}", result != null ? "title=" + result.getTitle() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询会话失败：conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteConversation(String conversationId) {
        log.debug("执行方法：deleteConversation, 参数：conversationId={}", conversationId);
        long startTime = System.currentTimeMillis();
        try {
            conversationMapper.deleteByConversationId(conversationId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("删除会话成功：conversationId={}, 耗时={}ms", conversationId, duration);
            log.debug("方法返回：void");
        } catch (Exception e) {
            log.error("删除会话失败：conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void updateConversationTitle(String conversationId, String title) {
        log.debug("执行方法：updateConversationTitle, 参数：conversationId={}, title={}", conversationId, title);
        long startTime = System.currentTimeMillis();
        try {
            conversationMapper.updateTitle(conversationId, title);
            long duration = System.currentTimeMillis() - startTime;
            log.info("更新会话标题成功：conversationId={}, newTitle={}, 耗时={}ms", conversationId, title, duration);
            log.debug("方法返回：void");
        } catch (Exception e) {
            log.error("更新会话标题失败：conversationId={}, title={}, error={}", conversationId, title, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String generateConversationId(String username) {
        log.debug("执行方法：generateConversationId, 参数：username={}", username);
        // 每个用户只有一个会话，序号固定为 1
        String conversationId = username + ":1";
        log.debug("方法返回：conversationId={}", conversationId);
        return conversationId;
    }

    @Override
    public Conversation getOrCreateUserConversation(String username) {
        log.debug("执行方法：getOrCreateUserConversation, 参数：username={}", username);
        long startTime = System.currentTimeMillis();
        try {
            // 尝试获取用户的唯一会话（序号为 1）
            String conversationId = username + ":1";
            Conversation existingConversation = conversationMapper.findByConversationId(conversationId);

            if (existingConversation != null) {
                log.info("获取已有会话：username={}, conversationId={}", username, conversationId);
                log.debug("方法返回：result={}", existingConversation);
                return existingConversation;
            }

            // 会话不存在，创建新会话
            log.info("会话不存在，创建新会话：username={}", username);
            Conversation conversation = new Conversation();
            conversation.setConversationId(conversationId);
            conversation.setUsername(username);
            conversation.setSequenceNum(1);
            conversation.setTitle("AI 助手对话");
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());

            conversationMapper.insert(conversation);

            long duration = System.currentTimeMillis() - startTime;
            log.info("创建新会话成功：username={}, conversationId={}, 耗时={}ms", username, conversationId, duration);
            log.debug("方法返回：result={}", conversation);
            return conversation;
        } catch (Exception e) {
            log.error("获取或创建用户会话失败：username={}, error={}", username, e.getMessage(), e);
            throw e;
        }
    }
}
