package com.ccut.common.service;

import com.ccut.common.entity.Course;
import com.ccut.common.mapper.CourseMapper;
import com.ccut.common.mapper.TeacherMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Resource
    CourseMapper courseMapper;
    @Resource
    TeacherMapper teacherMapper;

    // 添加课程
    public int addCourse(Course course, List<Integer> teacherIds) {
        int result = courseMapper.insert(course);
        if (teacherIds != null && !teacherIds.isEmpty()) {
            for (Integer teacherId : teacherIds) {
                courseMapper.addTeacher(course.getId(), teacherId);
            }
        }
        return result;
    }

    // 删除课程
    public int deleteCourse(Integer id) {
        courseMapper.deleteCourseTeachers(id);
        return courseMapper.deleteById(id);
    }

    // 修改课程基本信息
    public int updateCourse(Course course) {
        return courseMapper.updateById(course);
    }

    // 更新课程的教师关系
    public void updateCourseTeachers(Integer courseId, List<Integer> teacherIds) {
        courseMapper.deleteCourseTeachers(courseId);
        if (teacherIds != null && !teacherIds.isEmpty()) {
            for (Integer teacherId : teacherIds) {
                courseMapper.addTeacher(courseId, teacherId);
            }
        }
    }

    // 查询所有课程
    public List<Course> selectAll(Course course) {
        return courseMapper.findAllWithTeachers(course);
    }



}
