package com.ccut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnrollmentMapper {
    int upsert(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    java.util.List<com.ccut.entity.Course> findCoursesByStudentId(@Param("studentId") Long studentId);

    java.util.List<com.ccut.entity.Student> findStudentsByCourseId(@Param("courseId") Long courseId);

    /**
     * 学生退课（更新状态为dropped）
     */
    int drop(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    /**
     * 检查学生是否已选某课程
     * @return 选课记录数量
     */
    int countByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    /**
     * 批量插入选课记录
     */
    int batchInsert(@Param("courseId") Long courseId, @Param("studentIds") List<Long> studentIds);
}


