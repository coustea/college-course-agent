package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程统计数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatistics {
    private Long courseId;
    private String courseName;
    private Integer totalStudents;      // 课程总学生数
    private Integer completedStudents;   // 已完成学生数
    private Double averageCompletion;   // 平均完成率 (0-100)
    private Integer totalVideos;        // 视频总数
    private Integer totalDocuments;     // 文档总数
}
