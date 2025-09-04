package com.ccut.common.mapper;

import com.ccut.common.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper {

    int insert(Course course);

    int deleteById(@Param("id") Integer id);

    int updateById(Course course);


    int addTeacher(@Param("courseId") Integer courseId, @Param("teacherId") Integer teacherId);

    int deleteCourseTeachers(@Param("courseId") Integer courseId);
    List<Integer> findTeacherIdsByCourseId(@Param("courseId") Integer courseId);

    int deleteTeacherCourses(Integer id);


    List<Course> findAllWithTeachers(Course course);

    Course selectById(@Param("id") Integer id);
}