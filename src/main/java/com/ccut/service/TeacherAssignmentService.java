package com.ccut.service;

import com.ccut.entity.TeacherAssignment;

import java.util.List;

public interface TeacherAssignmentService {

    int insert(TeacherAssignment teacherAssignment);
    int update(TeacherAssignment teacherAssignment);
    int delete(Long assignmentId);
    List<TeacherAssignment> selectByTeacherId(Long teacherId);
    List<TeacherAssignment> selectByCourseId(Long courseId);
    List<TeacherAssignment> selectByTeacherIdAndCourseId(Long teacherId, Long courseId);
    List<TeacherAssignment> selectAll();
    List<TeacherAssignment> selectByClassName(String className);
}
