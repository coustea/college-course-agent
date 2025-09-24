package com.ccut.service.Impl;

import com.ccut.entity.TeacherAssignment;
import com.ccut.mapper.TeacherAssignmentMapper;
import com.ccut.service.TeacherAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    @Autowired
    private TeacherAssignmentMapper teacherAssignmentMapper;
    @Override
    public int insert(TeacherAssignment teacherAssignment) {
        return teacherAssignmentMapper.insert(teacherAssignment);
    }

    @Override
    public int update(TeacherAssignment teacherAssignment) {
        return teacherAssignmentMapper.update(teacherAssignment);
    }

    @Override
    public int delete(Long assignmentId) {
        return teacherAssignmentMapper.delete(assignmentId);
    }

    @Override
    public List<TeacherAssignment> selectByTeacherId(Long teacherId) {
        return teacherAssignmentMapper.selectByTeacherId(teacherId);
    }
}
