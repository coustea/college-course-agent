package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.entity.Enrollment;
import com.ccut.entity.Student;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.EnrollmentMapper;
import com.ccut.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional
    public Enrollment enroll(Long studentId, Long courseId) {
        log.debug("执行方法：enroll, 参数：studentId={}, courseId={}", studentId, courseId);
        long startTime = System.currentTimeMillis();
        try {
            // 检查课程是否存在且已发布
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!"published".equals(course.getPublishStatus())) {
                log.error("课程未发布，无法选课：courseId={}, publishStatus={}", courseId, course.getPublishStatus());
                throw new IllegalArgumentException("该课程尚未发布，无法选课");
            }

            // 检查学生是否已选过该课程
            if (isEnrolled(studentId, courseId)) {
                log.warn("重复选课：studentId={}, courseId={}", studentId, courseId);
                throw new IllegalArgumentException("您已选过该课程，请勿重复选课");
            }

            // 执行选课（学生自选，来源为'student'）
            enrollmentMapper.upsert(studentId, courseId, "student");
            log.info("学生选课成功：studentId={}, courseId={}", studentId, courseId);

            // 构造返回的选课记录
            Enrollment enrollment = new Enrollment();
            enrollment.setCourse(course);
            enrollment.setStudent(new Student()); // 这里可以填充完整的学生信息
            enrollment.getStudent().setId(studentId);
            enrollment.setStatus(Enrollment.Status.active);
            enrollment.setEnrollmentSource("student");
            enrollment.setEnrollmentDate(new java.sql.Timestamp(System.currentTimeMillis()));

            long duration = System.currentTimeMillis() - startTime;
            log.info("选课完成：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
            log.debug("方法返回：result=选课成功");
            return enrollment;
        } catch (Exception e) {
            log.error("选课失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean drop(Long studentId, Long courseId) {
        log.debug("执行方法：drop, 参数：studentId={}, courseId={}", studentId, courseId);
        long startTime = System.currentTimeMillis();
        try {
            int rows = enrollmentMapper.drop(studentId, courseId);
            long duration = System.currentTimeMillis() - startTime;
            if (rows > 0) {
                log.info("退课成功：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
                log.debug("方法返回：result=true");
                return true;
            }
            log.warn("退课失败：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
            log.debug("方法返回：result=false");
            return false;
        } catch (Exception e) {
            log.error("退课失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public int batchEnroll(Long courseId, Long teacherId, List<Long> studentIds) {
        log.debug("执行方法：batchEnroll, 参数：courseId={}, teacherId={}, studentIds count={}", 
                courseId, teacherId, studentIds != null ? studentIds.size() : 0);
        long startTime = System.currentTimeMillis();
        try {
            // 验证课程是否属于该教师
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!course.getTeacherId().equals(teacherId)) {
                log.error("无权为此课程添加学生：courseId={}, teacherId={}, courseTeacherId={}", 
                        courseId, teacherId, course.getTeacherId());
                throw new IllegalArgumentException("您无权为此课程添加学生");
            }

            // 批量添加学生
            if (studentIds == null || studentIds.isEmpty()) {
                log.warn("学生列表为空，无法批量选课");
                return 0;
            }

            int result = enrollmentMapper.batchInsert(courseId, studentIds);
            long duration = System.currentTimeMillis() - startTime;
            log.info("批量选课成功：courseId={}, studentCount={}, 耗时={}ms", courseId, studentIds.size(), duration);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("批量选课失败：courseId={}, teacherId={}, error={}", courseId, teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        log.debug("执行方法：getEnrollmentsByStudent, 参数：studentId={}", studentId);
        try {
            // 使用新方法获取包含选课来源的完整选课信息
            List<Enrollment> result = enrollmentMapper.findEnrollmentsWithCoursesByStudentId(studentId);
            log.info("查询学生选课记录成功：studentId={}, count={}", studentId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询学生选课记录失败：studentId={}, error={}", studentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        log.debug("执行方法：getEnrollmentsByCourse, 参数：courseId={}", courseId);
        try {
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

            log.info("查询课程选课记录成功：courseId={}, count={}", courseId, enrollments.size());
            log.debug("方法返回：result count={}", enrollments.size());
            return enrollments;
        } catch (Exception e) {
            log.error("查询课程选课记录失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isEnrolled(Long studentId, Long courseId) {
        log.debug("执行方法：isEnrolled, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            int count = enrollmentMapper.countByStudentAndCourse(studentId, courseId);
            boolean result = count > 0;
            log.debug("检查选课状态：studentId={}, courseId={}, isEnrolled={}", studentId, courseId, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("检查选课状态失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }
}
