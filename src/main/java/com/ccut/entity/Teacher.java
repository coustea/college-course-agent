package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
// 有参构造函数
@AllArgsConstructor
@NoArgsConstructor
public class Teacher extends  User{
    private String employeeNumber; // 员工编号
    private String name;// 教师姓名
    private String email;// 邮箱地址
    private String phone; // 联系电话
    private String department;// 所属部门
    private String title;// 职称

}
