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
        try {
            Long studentId = request.get("studentId");
            Long courseId = request.get("courseId");

            if (studentId == null || courseId == null) {
                return Result.error(400, "studentId和courseId不能为空");
            }

            Enrollment enrollment = enrollmentService.enroll(studentId, courseId);
            return Result.success(enrollment);
        } catch (IllegalArgumentException e) {
            log.warn("选课参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("选课业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("选课失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 学生退课
     */
    @PostMapping("/drop")
    public Result<String> drop(@RequestBody Map<String, Long> request) {
        try {
            Long studentId = request.get("studentId");
            Long courseId = request.get("courseId");

            if (studentId == null || courseId == null) {
                return Result.error(400, "studentId和courseId不能为空");
            }

            boolean success = enrollmentService.drop(studentId, courseId);
            if (success) {
                return Result.success("退课成功");
            }
            return Result.error(404, "未找到选课记录");
        } catch (Exception e) {
            log.error("退课失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 教师批量添加学生到课程
     */
    @PostMapping("/batch-enroll")
    public Result<String> batchEnroll(@RequestBody Map<String, Object> request) {
        try {
            Long courseId = ((Number) request.get("courseId")).longValue();
            Long teacherId = ((Number) request.get("teacherId")).longValue();
            @SuppressWarnings("unchecked")
            List<Long> studentIds = (List<Long>) request.get("studentIds");

            if (courseId == null || teacherId == null || studentIds == null || studentIds.isEmpty()) {
                return Result.error(400, "courseId、teacherId和studentIds不能为空");
            }

            int count = enrollmentService.batchEnroll(courseId, teacherId, studentIds);
            return Result.success("成功添加 " + count + " 名学生");
        } catch (IllegalArgumentException e) {
            log.warn("批量添加学生参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("批量添加学生业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("批量添加学生失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询学生的已选课程列表
     */
    @GetMapping("/student/{studentId}")
    public Result<List<Enrollment>> getEnrollmentsByStudent(@PathVariable("studentId") Long studentId) {
        try {
            return Result.success(enrollmentService.getEnrollmentsByStudent(studentId));
        } catch (Exception e) {
            log.error("查询学生选课列表失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询课程的所有选课学生
     */
    @GetMapping("/course/{courseId}/students")
    public Result<List<Enrollment>> getEnrollmentsByCourse(@PathVariable("courseId") Long courseId) {
        try {
            return Result.success(enrollmentService.getEnrollmentsByCourse(courseId));
        } catch (Exception e) {
            log.error("查询课程选课学生失败: {}", e.getMessage(), e);
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
        try {
            boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
            return Result.success(enrolled);
        } catch (Exception e) {
            log.error("检查选课状态失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
