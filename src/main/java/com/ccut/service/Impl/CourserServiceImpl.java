package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.mapper.CourseMapper;
import com.ccut.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourserServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Override
    public int insert(Course course) {
        return courseMapper.insert(course);
    }

    @Override
    public int deleteById(Long courseId) {
        return courseMapper.deleteById(courseId);
    }

    @Override
    public int updateById(Course course) {
        return courseMapper.updateById(course);
    }

    @Override
    public List<Course> selectAll() {
        return courseMapper.selectAll();
    }

    @Override
    public Course selectById(Long courseId) {
        return courseMapper.selectById(courseId);
    }
}
