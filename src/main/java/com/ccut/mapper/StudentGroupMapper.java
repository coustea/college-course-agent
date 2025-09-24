package com.ccut.mapper;

import com.ccut.entity.StudentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentGroupMapper {

    int insert(StudentGroup studentGroup);
    int update(StudentGroup studentGroup);
    
    List<StudentGroup> selectByApprovalStatus(@Param("approvalStatus") StudentGroup.GroupApprovalStatus status);
    
    List<StudentGroup> selectAll();
    StudentGroup selectByGroupId(@Param("groupId") Long groupId);
    int deleteById(@Param("id") Long id);
}