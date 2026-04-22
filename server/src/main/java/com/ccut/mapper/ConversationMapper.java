package com.ccut.mapper;

import com.ccut.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper {

    /**
     * 插入新会话
     */
    int insert(Conversation conversation);

    /**
     * 根据用户名查询该用户的所有会话
     */
    List<Conversation> findByUsername(@Param("username") String username);

    /**
     * 根据 conversation_id 查询会话
     */
    Conversation findByConversationId(@Param("conversationId") String conversationId);

    /**
     * 获取用户下一个会话序号
     */
    int getNextSequenceNum(@Param("username") String username);

    /**
     * 更新会话标题
     */
    int updateTitle(@Param("conversationId") String conversationId, @Param("title") String title);

    /**
     * 删除会话
     */
    int deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据用户ID查询会话
     */
    Conversation findByUserId(@Param("userId") Long userId);

    /**
     * 根据学生ID查询会话列表
     */
    List<Conversation> findByStudentId(@Param("studentId") Long studentId);
}