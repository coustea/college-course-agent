package com.ccut.service.Impl;

import com.ccut.entity.Teacher;
import com.ccut.mapper.TeacherMapper;
import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public int insert(Teacher teacher) {
        // 兜底 username/password/role
        String username = teacher.getUsername();
        if (username == null || username.isEmpty()) {
            username = teacher.getEmployeeNumber();
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username 或 employeeNumber 不能为空");
        }
        String password = teacher.getPassword();
        if (password == null || password.isEmpty()) {
            password = "123456";
        }
        // 先插入 users 获取自增 id
        User user = new User(username, password, User.Role.teacher);
        userMapper.insertUser(user);
        Long id = user.getId();
        teacher.setId(id);
        // 再插入 teachers 使用相同 id
        return teacherMapper.insertTeacher(teacher);
    }

    public int update(Teacher teacher) {
        return teacherMapper.updateById(teacher);
    }

    public int deleteById(Long id) {
        return teacherMapper.deleteById(id);
    }

    public java.util.List<Teacher> selectAll() {
        return teacherMapper.selectAll();
    }
}


