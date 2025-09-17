package com.ccut.mapper;

import com.ccut.entity.Course;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseMapper {
    int insert(Course course);
    int updateById(Course course);
    int deleteById(Long courseId);
    List<Course> selectAll();
    Course selectById(Long courseId);
    Course selectByCourseCode(@org.apache.ibatis.annotations.Param("courseCode") String courseCode);
    java.util.List<Course> search(
            @org.apache.ibatis.annotations.Param("name") String name,
            @org.apache.ibatis.annotations.Param("description") String description);

    // 新增：按教师ID返回课程卡片字段
    java.util.List<com.ccut.entity.TeacherCourseCard> listCourseCardsByTeacher(@org.apache.ibatis.annotations.Param("teacherId") Long teacherId);
}

