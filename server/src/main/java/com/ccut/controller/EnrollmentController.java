package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Enrollment;
import com.ccut.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 选课控制器
 */
@RestController
@RequestMapping("/api/enrollment")
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
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("studentId 和 courseId 不能为空");
        }
        Enrollment enrollment = enrollmentService.enroll(studentId, courseId);
        return Result.success(enrollment);
    }

    /**
     * 学生退课
     */
    @PostMapping("/drop")
    public Result<String> drop(@RequestBody Map<String, Long> request) {
        Long studentId = request.get("studentId");
        Long courseId = request.get("courseId");
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("studentId 和 courseId 不能为空");
        }
        boolean success = enrollmentService.drop(studentId, courseId);
        if (!success) {
            throw new RuntimeException("未找到选课记录");
        }
        return Result.success("退课成功");
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
        if (courseId == null || teacherId == null || studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException("courseId、teacherId 和 studentIds 不能为空");
        }
        int count = enrollmentService.batchEnroll(courseId, teacherId, studentIds);
        return Result.success("成功添加 " + count + " 名学生");
    }

    /**
     * 查询学生的已选课程列表
     */
    @GetMapping("/student/{studentId}")
    public Result<List<Enrollment>> getEnrollmentsByStudent(@PathVariable("studentId") Long studentId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(studentId);
        return Result.success(enrollments);
    }

    /**
     * 查询课程的所有选课学生
     */
    @GetMapping("/course/{courseId}/students")
    public Result<List<Enrollment>> getEnrollmentsByCourse(@PathVariable("courseId") Long courseId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByCourse(courseId);
        return Result.success(enrollments);
    }

    /**
     * 检查学生是否已选某课程
     */
    @GetMapping("/check")
    public Result<Boolean> isEnrolled(
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId) {
        boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
        return Result.success(enrolled);
    }

    /**
     * 通过Excel批量导入学生到课程
     * Excel文件应包含学号列（第一列或有"学号"标题的列）
     */
    @PostMapping("/import-excel")
    public Result<Map<String, Object>> importStudentsFromExcel(
            @RequestParam("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = enrollmentService.importStudentsFromExcel(courseId, teacherId, file);
        return Result.success(result);
    }
}
