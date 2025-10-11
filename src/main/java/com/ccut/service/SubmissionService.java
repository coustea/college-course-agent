package com.ccut.service;

import com.ccut.entity.StudentSubmission;

import java.util.List;
import java.util.Map;

public interface SubmissionService {

    int insert(StudentSubmission studentSubmission);
    StudentSubmission selectByGroupId(Long groupId);
    StudentSubmission selectByAssignmentIdAndGroupId(Long assignmentId, Long groupId);
    List<StudentSubmission> selectByAssignmentId(Long assignmentId);
    List<StudentSubmission> selectAll();
}
