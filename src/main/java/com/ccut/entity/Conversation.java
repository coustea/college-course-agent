package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    private Long id; // 数据库自增主键
    private String conversationId; // 业务唯一标识，格式：username:序号（如zhangsan:1）
    private String username; // 所属用户名
    private Integer sequenceNum; // 该用户的第几个会话（从1递增）
    private String title; // 会话标题，用户可自定义
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 最后更新时间（有新消息时自动更新）
}