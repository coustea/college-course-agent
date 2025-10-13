package com.ccut.mapper;

import com.ccut.entity.StudentSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentSubmissionMapper {

    int insert(StudentSubmission studentSubmission);
    int updateById(StudentSubmission studentSubmission);
    StudentSubmission selectByGroupId(@Param("groupId") Long groupId);
    StudentSubmission selectByAssignmentIdAndGroupId(@Param("assignmentId") Long assignmentId, @Param("groupId") Long groupId);
    List<StudentSubmission> selectByAssignmentId(@Param("assignmentId") Long assignmentId);
    List<StudentSubmission> selectAll();
}
