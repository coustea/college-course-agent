package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.entity.Teacher;
import com.ccut.entity.User;
import com.ccut.mapper.StudentMapper;
import com.ccut.mapper.TeacherMapper;
import com.ccut.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private TeacherMapper teacherMapper;

    @PostMapping("/login")
    public Result<Object> login(@RequestBody User login) {
        try {
            if (login.getUsername() == null || login.getPassword() == null) {
                return Result.error(400, "用户名和密码不能为空");
            }
            User db = userMapper.getUserByUsername(login.getUsername());
            if (db == null) return Result.error(401, "账号不存在");
            if (!db.getPassword().equals(login.getPassword())) return Result.error(401, "密码错误");

            // 根据角色返回所有信息
            if (db.getRole() == User.Role.student) {
                Student s = studentMapper.selectById(db.getId());
                // 补齐账号信息
                if (s != null) {
                    s.setUsername(db.getUsername());
                    s.setPassword(db.getPassword());
                    s.setRole(db.getRole());
                }
                return Result.success(s);
            } else if (db.getRole() == User.Role.teacher) {
                // 查询教师档案并补齐账号信息
                Teacher t = teacherMapper.selectById(db.getId());
                if (t == null) {
                    t = new Teacher();
                    t.setId(db.getId());
                }
                t.setUsername(db.getUsername());
                t.setPassword(db.getPassword());
                t.setRole(db.getRole());
                return Result.success(t);
            } else {
                return Result.success(db);
            }
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}


