package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Enrollment;
import com.ccut.service.EnrollmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 选课控制器
 */
@RestController
@RequestMapping("/api/enrollment")
@Slf4j
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * 学生选课
     */
    @PostMapping("/enroll")
    public Result<Enrollment> enroll(@RequestBody Map<String, Long> request) {
        Long studentId = request.get("studentId");
        Long courseId = request.get("courseId");
        log.debug("收到学生选课请求：URI=/api/enrollment/enroll, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            if (studentId == null || courseId == null) {
                log.warn("选课参数错误：studentId={}, courseId={}", studentId, courseId);
                return Result.error(400, "studentId 和 courseId 不能为空");
            }
            log.info("执行学生选课业务：studentId={}, courseId={}", studentId, courseId);
            Enrollment enrollment = enrollmentService.enroll(studentId, courseId);
            log.debug("学生选课成功：studentId={}, courseId={}, enrollmentId={}", studentId, courseId, enrollment.getEnrollmentId());
            return Result.success(enrollment);
        } catch (IllegalArgumentException e) {
            log.warn("选课参数错误：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("选课业务异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("选课失败：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 学生退课
     */
    @PostMapping("/drop")
    public Result<String> drop(@RequestBody Map<String, Long> request) {
        Long studentId = request.get("studentId");
        Long courseId = request.get("courseId");
        log.debug("收到学生退课请求：URI=/api/enrollment/drop, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            if (studentId == null || courseId == null) {
                log.warn("退课参数错误：studentId={}, courseId={}", studentId, courseId);
                return Result.error(400, "studentId 和 courseId 不能为空");
            }
            log.info("执行学生退课业务：studentId={}, courseId={}", studentId, courseId);
            boolean success = enrollmentService.drop(studentId, courseId);
            if (success) {
                log.debug("退课成功：studentId={}, courseId={}", studentId, courseId);
                return Result.success("退课成功");
            }
            log.warn("未找到选课记录：studentId={}, courseId={}", studentId, courseId);
            return Result.error(404, "未找到选课记录");
        } catch (Exception e) {
            log.error("退课失败：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 教师批量添加学生到课程
     */
    @PostMapping("/batch-enroll")
    public Result<String> batchEnroll(@RequestBody Map<String, Object> request) {
        Long courseId = request.get("courseId") != null ? ((Number) request.get("courseId")).longValue() : null;
        Long teacherId = request.get("teacherId") != null ? ((Number) request.get("teacherId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Long> studentIds = (List<Long>) request.get("studentIds");
        log.debug("收到批量添加学生请求：URI=/api/enrollment/batch-enroll, 参数：courseId={}, teacherId={}, studentIds 大小={}",
                courseId, teacherId, studentIds != null ? studentIds.size() : 0);
        try {
            if (courseId == null || teacherId == null || studentIds == null || studentIds.isEmpty()) {
                log.warn("批量添加学生参数错误：courseId={}, teacherId={}, studentIds={}", courseId, teacherId, studentIds);
                return Result.error(400, "courseId、teacherId 和 studentIds 不能为空");
            }
            log.info("执行批量添加学生业务：courseId={}, teacherId={}, studentCount={}", courseId, teacherId, studentIds.size());
            int count = enrollmentService.batchEnroll(courseId, teacherId, studentIds);
            log.debug("批量添加学生成功：courseId={}, 添加人数={}", courseId, count);
            return Result.success("成功添加 " + count + " 名学生");
        } catch (IllegalArgumentException e) {
            log.warn("批量添加学生参数错误：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("批量添加学生业务异常：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("批量添加学生失败：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询学生的已选课程列表
     */
    @GetMapping("/student/{studentId}")
    public Result<List<Enrollment>> getEnrollmentsByStudent(@PathVariable("studentId") Long studentId) {
        log.debug("收到查询学生选课列表请求：URI=/api/enrollment/student/{}, 参数：studentId={}", studentId, studentId);
        try {
            log.info("执行查询学生选课列表业务：studentId={}", studentId);
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(studentId);
            log.debug("查询学生选课列表成功：studentId={}, 课程数={}", studentId, enrollments.size());
            return Result.success(enrollments);
        } catch (Exception e) {
            log.error("查询学生选课列表失败：studentId={}, 错误：{}", studentId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询课程的所有选课学生
     */
    @GetMapping("/course/{courseId}/students")
    public Result<List<Enrollment>> getEnrollmentsByCourse(@PathVariable("courseId") Long courseId) {
        log.debug("收到查询课程选课学生请求：URI=/api/enrollment/course/{}/students, 参数：courseId={}", courseId, courseId);
        try {
            log.info("执行查询课程选课学生业务：courseId={}", courseId);
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByCourse(courseId);
            log.debug("查询课程选课学生成功：courseId={}, 学生数={}", courseId, enrollments.size());
            return Result.success(enrollments);
        } catch (Exception e) {
            log.error("查询课程选课学生失败：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 检查学生是否已选某课程
     */
    @GetMapping("/check")
    public Result<Boolean> isEnrolled(
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId) {
        log.debug("收到检查选课状态请求：URI=/api/enrollment/check, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行检查选课状态业务：studentId={}, courseId={}", studentId, courseId);
            boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
            log.debug("检查选课状态成功：studentId={}, courseId={}, enrolled={}", studentId, courseId, enrolled);
            return Result.success(enrolled);
        } catch (Exception e) {
            log.error("检查选课状态失败：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
