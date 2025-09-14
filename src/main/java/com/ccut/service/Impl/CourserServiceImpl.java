package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourserServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseVideoMapper courseVideoMapper;
    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

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
        List<Course> courses = courseMapper.selectAll();
        if (courses != null) {
            for (Course c : courses) {
                if (c != null && c.getCourseId() != null) {
                    c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                    c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                }
            }
        }
        return courses;
    }

    @Override
    public Course selectById(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        if (c != null && c.getCourseId() != null) {
            c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
            c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
        }
        return c;
    }

    @Override
    public List<Course> searchByName(String name) {
        return courseMapper.search(name, null);
    }

    public List<Course> search(String name, String description) {
        List<Course> courses = courseMapper.search(name, description);
        if (courses != null) {
            for (Course c : courses) {
                if (c != null && c.getCourseId() != null) {
                    c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                    c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                }
            }
        }
        return courses;
    }
}
