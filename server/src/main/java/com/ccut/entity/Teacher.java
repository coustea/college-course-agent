package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class Teacher extends  User{
    private String name;// 教师姓名
    private String email;// 邮箱地址
    private String phone; // 联系电话
    private String department;// 所属部门
    private String title;// 职称
    private String position;// 职务
    private String bio; // 教师简介

    public Teacher(Long id,String name){
        super.setId(id);
        this.name = name;
    }
}
