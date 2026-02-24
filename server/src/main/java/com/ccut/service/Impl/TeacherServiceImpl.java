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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TeacherServiceImpl.class);

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
        log.debug("执行方法：insert, 参数：teacher={}", teacher != null ? "name=" + teacher.getName() : "null");
        try {
            int result = teacherMapper.insertTeacher(teacher);
            log.info("教师插入成功：teacherId={}, result={}", teacher != null ? teacher.getId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("教师插入失败：teacherName={}, error={}", teacher != null ? teacher.getName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int update(Teacher teacher) {
        log.debug("执行方法：update, 参数：teacher={}", teacher != null ? "id=" + teacher.getId() : "null");
        try {
            int result = teacherMapper.updateById(teacher);
            log.info("教师信息更新成功：teacherId={}, result={}", teacher != null ? teacher.getId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("教师信息更新失败：teacherId={}, error={}", teacher != null ? teacher.getId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int deleteById(Long id) {
        log.debug("执行方法：deleteById, 参数：id={}", id);
        try {
            int result = teacherMapper.deleteById(id);
            log.info("教师删除成功：teacherId={}, result={}", id, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("教师删除失败：teacherId={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Teacher> selectAll() {
        log.debug("执行方法：selectAll");
        try {
            List<Teacher> result = teacherMapper.selectAll();
            log.info("查询所有教师成功：count={}", result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询所有教师失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Teacher selectById(Long id) {
        log.debug("执行方法：selectById, 参数：id={}", id);
        try {
            Teacher result = teacherMapper.selectById(id);
            log.debug("方法返回：result={}", result != null ? "id=" + result.getId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询教师失败：teacherId={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<String> selectClassNameByTeacherId(Long teacherId) {
        log.debug("执行方法：selectClassNameByTeacherId, 参数：teacherId={}", teacherId);
        try {
            List<String> result = teacherMapper.selectClassNameByTeacherId(teacherId);
            log.info("查询教师班级成功：teacherId={}, count={}", teacherId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询教师班级失败：teacherId={}, error={}", teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String enrollStudent(Long studentId, Long courseId) {
        log.debug("执行方法：enrollStudent, 参数：studentId={}, courseId={}", studentId, courseId);
        long startTime = System.currentTimeMillis();
        try {
            int n = enrollmentMapper.upsert(studentId, courseId, "teacher");
            long duration = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("教师代选课程成功：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
                log.debug("方法返回：result=选课成功");
                return "选课成功";
            }
            log.error("教师代选课程失败：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
            throw new RuntimeException("选课失败");
        } catch (Exception e) {
            log.error("教师代选课程异常：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Course> findCoursesByStudentId(Long studentId) {
        log.debug("执行方法：findCoursesByStudentId, 参数：studentId={}", studentId);
        try {
            List<Course> courses = enrollmentMapper.findCoursesByStudentId(studentId);
            log.info("查询学生课程成功：studentId={}, count={}", studentId, courses != null ? courses.size() : 0);
            if (courses != null) {
                for (Course c : courses) {
                    if (c != null && c.getCourseId() != null) {
                        c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                        c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                    }
                }
            }
            log.debug("方法返回：result count={}", courses != null ? courses.size() : 0);
            return courses;
        } catch (Exception e) {
            log.error("查询学生课程失败：studentId={}, error={}", studentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Student> findStudentsByCourseId(Long courseId) {
        log.debug("执行方法：findStudentsByCourseId, 参数：courseId={}", courseId);
        try {
            List<Student> result = enrollmentMapper.findStudentsByCourseId(courseId);
            log.info("查询课程学生成功：courseId={}, count={}", courseId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询课程学生失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Map<String, Object> listTeacherVideos(Long teacherId) {
        log.debug("执行方法：listTeacherVideos, 参数：teacherId={}", teacherId);
        long startTime = System.currentTimeMillis();
        try {
            if (teacherId == null) {
                log.error("参数验证失败：teacherId 不能为空");
                throw new IllegalArgumentException("teacherId 不能为空");
            }

            List<TeacherVideoItem> videos = courseVideoMapper.listByTeacherId(teacherId);
            List<TeacherCourseCard> courses = courseMapper.listCourseCardsByTeacher(teacherId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("videos", videos);
            resp.put("courses", courses);

            long duration = System.currentTimeMillis() - startTime;
            log.info("查询教师视频和课程成功：teacherId={}, videos={}, courses={}, 耗时={}ms", 
                    teacherId, videos != null ? videos.size() : 0, courses != null ? courses.size() : 0, duration);
            log.debug("方法返回：result={videos: {}, courses: {}}", 
                    videos != null ? videos.size() : 0, courses != null ? courses.size() : 0);
            return resp;
        } catch (Exception e) {
            log.error("查询教师视频和课程失败：teacherId={}, error={}", teacherId, e.getMessage(), e);
            throw e;
        }
    }

}
