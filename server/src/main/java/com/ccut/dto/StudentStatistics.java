package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生统计数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatistics {
    /**
     * 在修课程数量
     */
    private Integer courseCount;

    /**
     * 本周学习时长（小时）
     */
    private Double weeklyStudyHours;

    /**
     * 连续打卡天数
     */
    private Integer consecutiveDays;
}
