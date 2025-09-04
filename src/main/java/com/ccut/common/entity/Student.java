package com.ccut.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Student extends User{
    private String academe;
    private String classname;
    private String phone;

    public Student(Integer id, String username, String password, String name, String role, String token, String academe, String classname, String phone) {
        super(id, username, password, name, role, token);
        this.academe = academe;
        this.classname = classname;
        this.phone = phone;
    }
}
