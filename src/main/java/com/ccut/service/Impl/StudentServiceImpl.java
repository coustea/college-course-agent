package com.ccut.service.Impl;

import com.ccut.entity.Student;
import com.ccut.mapper.StudentMapper;
import com.ccut.mapper.UserMapper;
import com.ccut.entity.User;
import com.ccut.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
