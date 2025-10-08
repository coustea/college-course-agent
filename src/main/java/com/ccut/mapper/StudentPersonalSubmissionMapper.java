package com.ccut.mapper;

import com.ccut.entity.StudentPersonalSubmission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentPersonalSubmissionMapper {

    int insert(StudentPersonalSubmission submission);

    int upsert(StudentPersonalSubmission submission);

    int grade(Long assignmentId, Long studentId, Integer score, String feedback, Long gradedBy);

    java.util.List<StudentPersonalSubmission> listByAssignment(Long assignmentId);

    java.util.List<StudentPersonalSubmission> listByStudent(Long studentId);

    StudentPersonalSubmission findOne(Long assignmentId, Long studentId);
}


