-- 为 messages 表添加 files 字段，用于存储附件信息
-- 执行时间: 2024-02-24

ALTER TABLE messages
ADD COLUMN files TEXT NULL COMMENT '附件信息（JSON字符串，存储文件列表）'
AFTER content;

-- 添加索引以优化查询
ALTER TABLE messages
ADD INDEX idx_files ((255));
