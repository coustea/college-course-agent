package com.ccut.service.Impl;

import com.ccut.entity.Student;
import com.ccut.entity.StudentSubmission;
import com.ccut.mapper.StudentSubmissionMapper;
import com.ccut.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private StudentSubmissionMapper studentSubmissionMapper;

    @Autowired
    private StudentServiceImpl studentService;

    @Override
    public int insert(StudentSubmission studentSubmission) {
        return studentSubmissionMapper.insert(studentSubmission);
    }

    @Override
    public StudentSubmission selectByGroupId(Long groupId) {
        return studentSubmissionMapper.selectByGroupId(groupId);
    }

    @Override
    public StudentSubmission selectByAssignmentIdAndGroupId(Long assignmentId, Long groupId) {
        return studentSubmissionMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
    }

    @Override
    public List<StudentSubmission> selectByAssignmentId(Long assignmentId) {
        return studentSubmissionMapper.selectByAssignmentId(assignmentId);
    }

    @Override
    public List<StudentSubmission> selectAll() {
        return studentSubmissionMapper.selectAll();
    }

}
