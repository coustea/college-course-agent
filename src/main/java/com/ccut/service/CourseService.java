package com.ccut.service;

import com.ccut.entity.Course;

import java.util.List;

public interface CourseService {
    int insert(Course course);
    int deleteById(Long courseId);
    int updateById(Course course);
    List<Course> selectAll();
    Course selectById(Long courseId);
    List<Course> searchByName(String name);
    List<Course> search(String name, String description);
}


