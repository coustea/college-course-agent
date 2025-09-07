package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
// 自动生成getter和setter
@Data
// 有参构造函数
@NoArgsConstructor
public class User {

    public enum Role {
        student,
        teacher
    }

    private Long id;// 用户ID
    private String username;// 用户名（学号或工号）
    private String password;//用户密码（加密存储）
    private Role role;// 用户角色（学生/教师）

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
