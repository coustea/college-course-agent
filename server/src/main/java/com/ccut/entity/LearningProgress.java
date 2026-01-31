package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgress {
    private Long progressId;                 // 学习进度ID
    private Long studentId;                  // 学生ID（对应 students.id）
    private Long courseId;                   // 课程ID（对应 courses.course_id）
    private Boolean completed;               // 是否完成
    private Double completionPercentage;     // 完成百分比 0.00~100.00
    private Integer timeSpent;               // 累计学习时间（秒）
    private Date lastStudyTime;              // 最后一次学习时间
}
