package com.ccut.service.Impl;

import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
import com.ccut.service.StudentGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {

    @Autowired
    private StudentGroupMapper studentGroupMapper;

    @Override
    public int insert(StudentGroup studentGroup) {
        return studentGroupMapper.insert(studentGroup);
    }

    @Override
    public int update(StudentGroup studentGroup) {
        return studentGroupMapper.update(studentGroup);
    }

    @Override
    public List<StudentGroup> selectByApprovalStatus(StudentGroup.GroupApprovalStatus approvalStatus) {
        return studentGroupMapper.selectByApprovalStatus(approvalStatus);
    }

    @Override
    public List<StudentGroup> selectAll() {
        return studentGroupMapper.selectAll();
    }

    @Override
    public StudentGroup selectByGroupId(Long groupId) {
        return studentGroupMapper.selectByGroupId(groupId);
    }

    @Override
    public int deleteById(Long id) {
        return studentGroupMapper.deleteById(id);
    }

    @Override
    public List<StudentGroup> selectByCourseIdAndTeacherId(Long courseId, Long teacherId) {
        return studentGroupMapper.selectByCourseIdAndTeacherId(courseId, teacherId);
    }
}