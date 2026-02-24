-- 创建课程推荐表
CREATE TABLE IF NOT EXISTS `recommendation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '推荐ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `reason` VARCHAR(500) COMMENT '推荐理由',
    `score` DOUBLE NOT NULL COMMENT '推荐分数（0-1）',
    `recommendation_type` VARCHAR(50) NOT NULL COMMENT '推荐类型：CONTENT_BASED, COLLABORATIVE, POPULAR',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `has_clicked` BOOLEAN DEFAULT FALSE COMMENT '是否已点击',
    `clicked_at` DATETIME COMMENT '点击时间',
    PRIMARY KEY (`id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_student_score` (`student_id`, `score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程推荐表';
