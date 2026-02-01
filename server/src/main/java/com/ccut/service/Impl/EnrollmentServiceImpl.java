package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.entity.Enrollment;
import com.ccut.entity.Student;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.EnrollmentMapper;
import com.ccut.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 选课服务实现类
 */
@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional
    public Enrollment enroll(Long studentId, Long courseId) {
        // 检查课程是否存在且已发布
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (!"published".equals(course.getPublishStatus())) {
            throw new IllegalArgumentException("该课程尚未发布，无法选课");
        }

        // 检查学生是否已选过该课程
        if (isEnrolled(studentId, courseId)) {
            throw new IllegalArgumentException("您已选过该课程，请勿重复选课");
        }

        // 执行选课
        enrollmentMapper.upsert(studentId, courseId);

        // 构造返回的选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(new Student()); // 这里可以填充完整的学生信息
        enrollment.getStudent().setId(studentId);
        enrollment.setStatus(Enrollment.Status.active);
        enrollment.setEnrollmentDate(new java.sql.Timestamp(System.currentTimeMillis()));

        return enrollment;
    }

    @Override
    @Transactional
    public boolean drop(Long studentId, Long courseId) {
        int rows = enrollmentMapper.drop(studentId, courseId);
        return rows > 0;
    }

    @Override
    @Transactional
    public int batchEnroll(Long courseId, Long teacherId, List<Long> studentIds) {
        // 验证课程是否属于该教师
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("您无权为此课程添加学生");
        }

        // 批量添加学生
        if (studentIds == null || studentIds.isEmpty()) {
            return 0;
        }

        return enrollmentMapper.batchInsert(courseId, studentIds);
    }

    @Override
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        List<Course> courses = enrollmentMapper.findCoursesByStudentId(studentId);
        List<Enrollment> enrollments = new ArrayList<>();

        for (Course course : courses) {
            Enrollment enrollment = new Enrollment();
            enrollment.setCourse(course);
            enrollment.setStudent(new Student());
            enrollment.getStudent().setId(studentId);
            enrollment.setStatus(Enrollment.Status.active);
            enrollments.add(enrollment);
        }

        return enrollments;
    }

    @Override
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        List<Student> students = enrollmentMapper.findStudentsByCourseId(courseId);
        List<Enrollment> enrollments = new ArrayList<>();

        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            for (Student student : students) {
                Enrollment enrollment = new Enrollment();
                enrollment.setCourse(course);
                enrollment.setStudent(student);
                enrollment.setStatus(Enrollment.Status.active);
                enrollments.add(enrollment);
            }
        }

        return enrollments;
    }

    @Override
    public boolean isEnrolled(Long studentId, Long courseId) {
        int count = enrollmentMapper.countByStudentAndCourse(studentId, courseId);
        return count > 0;
    }
}
