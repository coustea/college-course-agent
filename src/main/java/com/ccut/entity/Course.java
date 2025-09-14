package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
@Data
// 有参构造函数
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String description;
    private Integer credits; // 学分
    private Teacher teacher;
    private Long teacherId; // 授课教师ID（外键）
    private Date startDate;
    private Date endDate;
    private String semester;// 学期
    private Integer maxStudents;// 最大选课人数
    private String resourceUrl; // 教学资源URL
    private Integer vindex; // 展示排序/权重（来自 SQL 表）
    private List<CourseVideo> videos; // 课程下的视频列表
    private List<CourseDocument> documents; // 课程下的文档列表

}
