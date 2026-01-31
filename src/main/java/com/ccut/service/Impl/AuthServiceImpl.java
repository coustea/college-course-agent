package com.ccut.service.Impl;

import com.ccut.entity.User;
import com.ccut.entity.Teacher;
import com.ccut.entity.Student;
import com.ccut.mapper.UserMapper;
import com.ccut.mapper.TeacherMapper;
import com.ccut.mapper.StudentMapper;
import com.ccut.service.AuthService;
import com.ccut.utils.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    @Transactional
    public Map<String, Object> login(User user) {
        // 基础参数验证
        if (user.getUsername() == null || user.getUsername().isEmpty()
                || user.getPassword() == null || user.getPassword().isEmpty()) {
            log.warn("用户名或密码为空");
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        // 查询数据库用户
        User dbUser = userMapper.getUserByUsername(user.getUsername());
        if (dbUser == null) {
            log.warn("账号不存在: {}", user.getUsername());
            throw new RuntimeException("账号不存在");
        }
        log.info("用户登录: userId={}, username={}", dbUser.getId(), dbUser.getUsername());

        // 密码校验
        if (!user.getPassword().equals(dbUser.getPassword())) {
            log.warn("密码错误: {}", user.getUsername());
            throw new RuntimeException("密码错误");
        }

        // 角色校验：确保前端选择的角色与数据库中的角色一致
        if (user.getRole() != null) {
            String frontendRole = user.getRole().toString().toLowerCase();
            String dbRole = dbUser.getRole().toString().toLowerCase();
            if (!frontendRole.equals(dbRole)) {
                log.warn("角色不匹配: 用户={}, 数据库角色={}, 前端选择角色={}",
                        user.getUsername(), dbRole, frontendRole);
                throw new RuntimeException("角色选择错误，请选择正确的角色");
            }
        }

        // 生成 Token
        String token = JWTUtils.generateToken(dbUser.getUsername());
        log.info("用户 {} 登录成功", dbUser.getUsername());

        // 返回前端的数据（用户名 + token + role + 角色信息）
        Map<String, Object> data = new HashMap<>();
        data.put("userId", dbUser.getId());
        data.put("username", dbUser.getUsername());
        data.put("token", token);

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

        // 更新用户 token
        dbUser.setToken(token);
        userMapper.updateUser(dbUser);

        return data;
    }

    @Override
    @Transactional
    public void logout(String token) {
        // 验证token有效性
        if (!JWTUtils.validateToken(token)) {
            throw new IllegalArgumentException("无效的token");
        }

        // 从token中解析用户名
        String username = JWTUtils.getUsernameFromToken(token);

        // 查询数据库用户
        User dbUser = userMapper.getUserByUsername(username);
        if (dbUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 清除用户的token
        dbUser.setToken(null);
        userMapper.updateUser(dbUser);

        log.info("用户 {} 退出登录成功", username);
    }

}
