package com.ccut.service.Impl;

import com.ccut.dto.TeacherCourseCard;
import com.ccut.dto.TeacherVideoItem;
import com.ccut.entity.Course;
import com.ccut.entity.Student;
import com.ccut.entity.Teacher;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.EnrollmentMapper;
import com.ccut.mapper.TeacherMapper;
import com.ccut.mapper.UserMapper;
import com.ccut.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师服务实现类
 */
@Service
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Override
    public int insert(Teacher teacher) {
        return teacherMapper.insertTeacher(teacher);
    }

    @Override
    public int update(Teacher teacher) {
        return teacherMapper.updateById(teacher);
    }

    @Override
    public int deleteById(Long id) {
        return teacherMapper.deleteById(id);
    }

    @Override
    public List<Teacher> selectAll() {
        return teacherMapper.selectAll();
    }

    @Override
    public Teacher selectById(Long id) {
        return teacherMapper.selectById(id);
    }

    @Override
    public List<String> selectClassNameByTeacherId(Long teacherId) {
        return teacherMapper.selectClassNameByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public String enrollStudent(Long studentId, Long courseId) {
        int n = enrollmentMapper.upsert(studentId, courseId);
        if (n > 0) {
            return "选课成功";
        }
        throw new RuntimeException("选课失败");
    }

    @Override
    public List<Course> findCoursesByStudentId(Long studentId) {
        List<Course> courses = enrollmentMapper.findCoursesByStudentId(studentId);
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
    public List<Student> findStudentsByCourseId(Long courseId) {
        return enrollmentMapper.findStudentsByCourseId(courseId);
    }

    @Override
    public Map<String, Object> listTeacherVideos(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId 不能为空");
        }

        List<TeacherVideoItem> videos = courseVideoMapper.listByTeacherId(teacherId);
        List<TeacherCourseCard> courses = courseMapper.listCourseCardsByTeacher(teacherId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("videos", videos);
        resp.put("courses", courses);
        return resp;
    }

}
