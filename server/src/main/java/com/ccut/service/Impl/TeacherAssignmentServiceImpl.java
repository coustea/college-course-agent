package com.ccut.service.Impl;

import com.ccut.entity.TeacherAssignment;
import com.ccut.mapper.TeacherAssignmentMapper;
import com.ccut.service.TeacherAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教师作业服务实现类
 */
@Service
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TeacherAssignmentServiceImpl.class);

    @Autowired
    private TeacherAssignmentMapper teacherAssignmentMapper;

    @Override
    public int insert(TeacherAssignment teacherAssignment) {
        log.debug("执行方法：insert, 参数：teacherAssignment={}", teacherAssignment != null ? "assignmentId=" + teacherAssignment.getAssignmentId() : "null");
        try {
            int result = teacherAssignmentMapper.insert(teacherAssignment);
            log.info("作业插入成功：assignmentId={}, result={}", teacherAssignment != null ? teacherAssignment.getAssignmentId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("作业插入失败：assignmentName={}, error={}", teacherAssignment != null ? teacherAssignment.getAssignmentName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int update(TeacherAssignment teacherAssignment) {
        log.debug("执行方法：update, 参数：teacherAssignment={}", teacherAssignment != null ? "assignmentId=" + teacherAssignment.getAssignmentId() : "null");
        try {
            int result = teacherAssignmentMapper.update(teacherAssignment);
            log.info("作业更新成功：assignmentId={}, result={}", teacherAssignment != null ? teacherAssignment.getAssignmentId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("作业更新失败：assignmentId={}, error={}", teacherAssignment != null ? teacherAssignment.getAssignmentId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int delete(Long assignmentId) {
        log.debug("执行方法：delete, 参数：assignmentId={}", assignmentId);
        try {
            int result = teacherAssignmentMapper.delete(assignmentId);
            log.info("作业删除成功：assignmentId={}, result={}", assignmentId, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("作业删除失败：assignmentId={}, error={}", assignmentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<TeacherAssignment> selectByTeacherId(Long teacherId) {
        log.debug("执行方法：selectByTeacherId, 参数：teacherId={}", teacherId);
        try {
            List<TeacherAssignment> result = teacherAssignmentMapper.selectByTeacherId(teacherId);
            log.info("查询教师作业成功：teacherId={}, count={}", teacherId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询教师作业失败：teacherId={}, error={}", teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<TeacherAssignment> selectByCourseId(Long courseId) {
        log.debug("执行方法：selectByCourseId, 参数：courseId={}", courseId);
        try {
            List<TeacherAssignment> result = teacherAssignmentMapper.selectByCourseId(courseId);
            log.info("查询课程作业成功：courseId={}, count={}", courseId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询课程作业失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<TeacherAssignment> selectByTeacherIdAndCourseId(Long teacherId, Long courseId) {
        log.debug("执行方法：selectByTeacherIdAndCourseId, 参数：teacherId={}, courseId={}", teacherId, courseId);
        try {
            List<TeacherAssignment> result = teacherAssignmentMapper.selectByTeacherIdAndCourseId(teacherId, courseId);
            log.info("查询教师课程作业成功：teacherId={}, courseId={}, count={}", teacherId, courseId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询教师课程作业失败：teacherId={}, courseId={}, error={}", teacherId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<TeacherAssignment> selectAll() {
        log.debug("执行方法：selectAll");
        try {
            List<TeacherAssignment> result = teacherAssignmentMapper.selectAll();
            log.info("查询所有作业成功：count={}", result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询所有作业失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<TeacherAssignment> selectByClassName(String className) {
        log.debug("执行方法：selectByClassName, 参数：className={}", className);
        try {
            List<TeacherAssignment> result = teacherAssignmentMapper.selectByClassName(className);
            log.info("按班级查询作业成功：className={}, count={}", className, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("按班级查询作业失败：className={}, error={}", className, e.getMessage(), e);
            throw e;
        }
    }
}
