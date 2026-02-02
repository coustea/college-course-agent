package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 每周学习时间统计实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStudyTime {
    private Long id;                    // 主键ID
    private Long studentId;             // 学生ID
    private Long courseId;              // 课程ID（为NULL表示总学习时间）
    private Date weekStartDate;         // 本周开始日期（周一的日期）
    private Integer totalSeconds;       // 本周累计学习时间（秒）
    private Date lastStudyTime;         // 本周最后一次学习时间
    private Date createdAt;             // 创建时间
    private Date updatedAt;             // 更新时间
}
