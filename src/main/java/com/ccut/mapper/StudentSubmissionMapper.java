package com.ccut.mapper;

import com.ccut.entity.StudentSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentSubmissionMapper {

    int insert(StudentSubmission studentSubmission);

    StudentSubmission selectById(@Param("submissionId") Long submissionId);

    java.util.List<StudentSubmission> selectByAssignmentId(@Param("assignmentId") Long assignmentId);

    java.util.List<StudentSubmission> selectByGroupId(@Param("groupId") Long groupId);

}
