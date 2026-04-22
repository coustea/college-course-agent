package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习路径记录实体
 * 追踪学生学习轨迹，用于行为分析
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathRecord {
    private Long recordId;              // 记录ID
    private Long studentId;             // 学生ID
    private Long courseId;              // 课程ID

    // 资源信息
    private String resourceType;        // 资源类型：video/document/ideology/exam
    private Long resourceId;            // 资源ID
    private String resourceTitle;       // 资源标题

    // 行为信息
    private String actionType;          // 行为类型：view/start/complete/interact/review
    private Integer durationSeconds;    // 持续时间（秒）
    private Double progressPercent;     // 进度百分比

    // 上下文信息
    private Long previousResourceId;    // 前一个资源ID（用于构建路径）
    private String deviceType;          // 设备类型：pc/mobile/tablet
    private String sessionId;           // 会话ID

    private LocalDateTime createdAt;    // 创建时间
}
