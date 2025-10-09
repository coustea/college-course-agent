package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student extends User{

    public enum Status{
        // 在校
        IN_SCHOOL,
        // 校外实习
        OFF_CAMPUS_INTERNSHIP
    }
    private String studentNumber; // 学号
    private String name; // 学生姓名
    private String className;// 班级
    private String email;// 邮箱地址
    private String phone;// 联系电话
    private String major; // 专业
    private String grade; // 年级
    private Status status;// 学生状态
    private Integer enrollmentYear;// 入学年份
}
