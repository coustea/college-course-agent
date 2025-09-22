package com.ccut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EnrollmentMapper {
    int upsert(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    java.util.List<com.ccut.entity.Course> findCoursesByStudentId(@Param("studentId") Long studentId);

    java.util.List<com.ccut.entity.Student> findStudentsByCourseId(@Param("courseId") Long courseId);
}


