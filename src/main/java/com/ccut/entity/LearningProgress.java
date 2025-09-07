package com.ccut.entity;

import jdk.jfr.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 有参构造函数
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LearningProgress {
    private Long progressId;// 学习进度ID
    private Student student;
    private Course course;
    private Boolean completed; // 是否完成
    private Double completionPercentage;// 完成百分比
    private Integer timeSpent;// 累计学习时间（秒）
}
