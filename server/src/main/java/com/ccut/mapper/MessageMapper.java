package com.ccut.mapper;

import com.ccut.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息 Mapper 接口
 */
@Mapper
public interface MessageMapper {

    /**
     * 插入新消息
     */
    int insert(Message message);

    /**
     * 根据会话ID查询所有消息（按序号排序）
     */
    List<Message> findByConversationId(@Param("conversationId") String conversationId);

    /**
     * 统计会话的消息数量
     */
    int countByConversationId(@Param("conversationId") String conversationId);

    /**
     * 删除会话的所有消息
     */
    int deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 获取会话下一个消息序号
     */
    int getNextSequenceNum(@Param("conversationId") String conversationId);
}
