package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
// 有参构造函数
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    private Long courseId;
    private String courseName;
    private String description;
    private Integer credits; // 学分
    private Teacher teacher;
    private Date startDate;
    private Date endDate;
    private String semester;// 学期
    private Integer maxStudents;// 最大选课人数

}
