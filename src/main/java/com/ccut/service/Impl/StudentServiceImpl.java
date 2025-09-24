package com.ccut.service.Impl;

import com.ccut.entity.Student;
import com.ccut.mapper.StudentMapper;
import com.ccut.mapper.UserMapper;
import com.ccut.entity.User;
import com.ccut.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserMapper userMapper;


    @Override
    public Student getStudentByStudentNumber(String studentNumber) {
        return studentMapper.getStudentByStudentNumber(studentNumber);
    }

    @Override
    public int update(Student student) {
        return studentMapper.updateById(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(Student student) {
        // 0) 兜底与校验
        String username = student.getUsername();
        if (username == null || username.isEmpty()) {
            username = student.getStudentNumber();
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username 或 studentNumber 不能为空");
        }
        String password = student.getPassword();
        if (password == null || password.isEmpty()) {
            password = "123456";
        }
        // 回写给 student，保持两表一致
        student.setUsername(username);
        student.setPassword(password);

        // 1) 先插入 users，生成主键 id
        User user = new User(username, password, User.Role.student);
        userMapper.insertUser(user);
        // 确保拿到自增的 id
        Long generatedId = user.getId();
        student.setId(generatedId);
        // 2) 再插入 students，使用与 users 相同的 id 作为外键
        return studentMapper.insertStudent(student);
    }

    public int deleteById(Long id) {
        return studentMapper.deleteById(id);
    }

    public java.util.List<Student> selectAll() {
        return studentMapper.selectAll();
    }

    @Override
    public Student selectById(Long id) {
        return studentMapper.selectById(id);
    }

    @Override
    public java.util.List<Student> selectByGrade(String grade) {
        return studentMapper.selectByGrade(grade);
    }
}
