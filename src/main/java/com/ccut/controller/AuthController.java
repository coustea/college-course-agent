package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.User;
import com.ccut.entity.Teacher;
import com.ccut.entity.Student;
import com.ccut.mapper.UserMapper;
import com.ccut.mapper.TeacherMapper;
import com.ccut.mapper.StudentMapper;
import com.ccut.utils.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody User user) {
        try {
            // 基础参数验证
            if (user.getUsername() == null || user.getUsername().isEmpty()
                    || user.getPassword() == null || user.getPassword().isEmpty()) {
                log.warn("用户名或密码为空");
                return Result.error(400, "用户名和密码不能为空");
            }

            // 查询数据库用户
            User dbUser = userMapper.getUserByUsername(user.getUsername());
            log.info("userId,{}", dbUser.getId());
            if (dbUser == null) {
                log.warn("账号不存在: {}", user.getUsername());
                return Result.error(401, "账号不存在");
            }

            //  密码校验
            if (!dbUser.getPassword().equals(user.getPassword())) {
                log.warn("密码错误: {}", user.getUsername());
                return Result.error(401, "密码错误");
            }

            // 生成 Token
            String token = JWTUtils.generateToken(dbUser.getUsername());
            log.info("用户 {} 登录成功", dbUser.getUsername());

            //返回前端的数据（用户名 + token + role + 角色信息）
            Map<String, Object> data = new HashMap<>();
            data.put("userId", dbUser.getId());
            data.put("username", dbUser.getUsername());
            data.put("token", token);
            try {
                // 将后端枚举角色转为前端可用的小写字符串：teacher/student
                String roleStr = dbUser.getRole().toString().toLowerCase();
                data.put("role", roleStr);

                // 附带角色详细信息（不返回敏感字段）
                Map<String, Object> profile = new HashMap<>();
                if ("teacher".equals(roleStr)) {
                    Teacher teacher = teacherMapper.selectById(dbUser.getId());
                    if (teacher != null) {
                        data.put("teacherId", teacher.getId());

                        profile.put("name", teacher.getName());
                        profile.put("email", teacher.getEmail());
                        profile.put("phone", teacher.getPhone());
                        profile.put("department", teacher.getDepartment());
                        profile.put("title", teacher.getTitle());
                    }
                } else if ("student".equals(roleStr)) {
                    Student student = studentMapper.selectById(dbUser.getId());

                    System.out.println(student);
                    if (student != null) {
                        data.put("studentId", student.getId());
                        profile.put("studentNumber", student.getStudentNumber());
                        profile.put("name", student.getName());
                        profile.put("className", student.getClassName());
                        profile.put("email", student.getEmail());
                        profile.put("phone", student.getPhone());
                        profile.put("major", student.getMajor());
                        profile.put("grade", student.getGrade());

                    }
                }
                data.put("profile", profile);
            } catch (Exception ignore) {}


            // 更新用户 token
            dbUser.setToken(token);
            userMapper.updateUser(dbUser);

            return Result.success(data);

        } catch (Exception e) {
            log.error("登录异常", e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }


    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    public Result<Object> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // 从Authorization头中提取token
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return Result.error(400, "无效的Authorization头");
            }

            String token = authorizationHeader.substring(7);

            // 验证token有效性
            if (!JWTUtils.validateToken(token)) {
                return Result.error(401, "无效的token");
            }

            // 从token中解析用户名
            String username = JWTUtils.getUsernameFromToken(token);

            // 查询数据库用户
            User dbUser = userMapper.getUserByUsername(username);
            if (dbUser == null) {
                return Result.error(404, "用户不存在");
            }

            // 清除用户的token
            dbUser.setToken(null);
            userMapper.updateUser(dbUser);

            log.info("用户 {} 退出登录成功", username);
            return Result.success("退出登录成功");

        } catch (Exception e) {
            log.error("退出登录异常", e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

}
