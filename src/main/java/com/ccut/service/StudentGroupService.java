package com.ccut.service;

import com.ccut.entity.StudentGroup;

import java.util.List;

public interface StudentGroupService {

    int insert(StudentGroup studentGroup);
    int update(StudentGroup studentGroup);
    List<StudentGroup> selectByApprovalStatus(StudentGroup.GroupApprovalStatus approvalStatus);
    
    List<StudentGroup> selectAll();
    StudentGroup selectByGroupId(Long groupId);
    int deleteById(Long id);
}