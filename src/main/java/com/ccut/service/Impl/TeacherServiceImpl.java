package com.ccut.service.Impl;

import com.ccut.entity.Teacher;
import com.ccut.mapper.TeacherMapper;
import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public int insert(Teacher teacher) {
        return teacherMapper.insertTeacher(teacher);
    }

    public int update(Teacher teacher) {
        return teacherMapper.updateById(teacher);
    }

    @Override
    public int deleteById(Long id) {
        return teacherMapper.deleteById(id);
    }

    public List<Teacher> selectAll() {
        return teacherMapper.selectAll();
    }

    @Override
    public List<String> selectClassNameByTeacherId(Long teacherId) {
        return teacherMapper.selectClassNameByTeacherId(teacherId);
    }
}


