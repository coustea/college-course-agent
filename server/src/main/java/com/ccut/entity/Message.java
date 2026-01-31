package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 聊天消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id; // 数据库自增主键
    private String conversationId; // 关联的会话ID（外键）
    private String role; // 消息角色：user(用户)/assistant(AI)/system(系统)
    private String content; // 消息内容（TEXT类型，支持长文本）
    private Integer sequenceNum; // 消息序号，同一会话内递增，用于排序和上下文
    private LocalDateTime createdAt; // 消息创建时间
    private Integer tokensUsed; // 该消息消耗的token数（用于成本统计）
}
