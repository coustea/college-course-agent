package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学情报告控制器
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取学生学习报告
     */
    @GetMapping("/student/{studentId}")
    public Result<ReportService.StudentLearningReport> getStudentReport(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(reportService.generateStudentReport(studentId, courseId));
    }

    /**
     * 获取课程学情报告
     */
    @GetMapping("/course/{courseId}")
    public Result<ReportService.CourseLearningReport> getCourseReport(
            @PathVariable Long courseId,
            @RequestParam(value = "period", required = false) String period) {
        return Result.success(reportService.generateCourseReport(courseId, period));
    }

    /**
     * 获取学生周报
     */
    @GetMapping("/student/{studentId}/weekly")
    public Result<ReportService.WeeklyReport> getWeeklyReport(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(reportService.getStudentWeeklyReport(studentId, courseId));
    }

    /**
     * 批量生成课程学生报告
     */
    @PostMapping("/course/{courseId}/batch")
    public Result<List<ReportService.StudentLearningReport>> batchGenerateReports(@PathVariable Long courseId) {
        return Result.success(reportService.batchGenerateStudentReports(courseId));
    }
}