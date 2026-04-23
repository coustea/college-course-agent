package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.DocumentProgress;
import com.ccut.entity.LearningProgress;
import com.ccut.entity.VideoProgress;
import com.ccut.service.ProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习进度控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    @PostMapping("/report")
    public Result<String> report(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId,
                                 @RequestParam(value = "videoId", required = false) Long videoId,
                                 @RequestParam(value = "documentId", required = false) Long documentId,
                                 @RequestParam(value = "deltaSec", defaultValue = "0") Integer deltaSec,
                                 @RequestParam(value = "scrollPct", required = false) Double scrollPct,
                                 @RequestParam(value = "completed", defaultValue = "false") Boolean completed) {
        progressService.reportProgress(studentId, courseId, videoId, documentId, deltaSec, scrollPct, completed);
        return Result.success("ok");
    }

    @PostMapping("/course/heartbeat")
    public Result<String> heartbeat(@RequestBody Map<String, Object> body) {
        Long courseId = body.get("courseId") != null ? Long.valueOf(body.get("courseId").toString()) : null;
        Integer deltaSec = body.get("deltaSec") != null ? Integer.valueOf(body.get("deltaSec").toString()) : 0;
        Long videoId = body.get("videoIndex") != null ? Long.valueOf(body.get("videoIndex").toString()) : null;

        // 从 JWT 获取 studentId
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        progressService.reportProgress(studentId, courseId, videoId, null, deltaSec, null, false);
        return Result.success("ok");
    }

    @GetMapping("/course")
    public Result<LearningProgress> getCourseProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId) {
        LearningProgress progress = progressService.getCourseProgress(studentId, courseId);
        return Result.success(progress);
    }

    @GetMapping("/video")
    public Result<VideoProgress> getVideoProgress(@RequestParam("studentId") Long studentId,
                                                  @RequestParam("courseId") Long courseId,
                                                  @RequestParam("videoId") Long videoId) {
        VideoProgress progress = progressService.getVideoProgress(studentId, courseId, videoId);
        return Result.success(progress);
    }

    @GetMapping("/video/list")
    public Result<List<VideoProgress>> listVideoProgress(@RequestParam("studentId") Long studentId,
                                                         @RequestParam("courseId") Long courseId) {
        List<VideoProgress> progresses = progressService.listVideoProgress(studentId, courseId);
        return Result.success(progresses);
    }

    @GetMapping("/document")
    public Result<DocumentProgress> getDocumentProgress(@RequestParam("studentId") Long studentId,
                                                        @RequestParam("courseId") Long courseId,
                                                        @RequestParam("documentId") Long documentId) {
        DocumentProgress progress = progressService.getDocumentProgress(studentId, courseId, documentId);
        return Result.success(progress);
    }

    @GetMapping("/document/list")
    public Result<List<DocumentProgress>> listDocumentProgress(@RequestParam("studentId") Long studentId,
                                                               @RequestParam("courseId") Long courseId) {
        List<DocumentProgress> progresses = progressService.listDocumentProgress(studentId, courseId);
        return Result.success(progresses);
    }

    @GetMapping("/course/all")
    public Result<Map<String, Object>> getAllProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId) {
        Map<String, Object> progress = progressService.getAllProgress(studentId, courseId);
        return Result.success(progress);
    }

    @GetMapping("/student/statistics")
    public Result<com.ccut.dto.StudentStatistics> getStudentStatistics(@RequestParam("studentId") Long studentId) {
        com.ccut.dto.StudentStatistics statistics = progressService.getStudentStatistics(studentId);
        return Result.success(statistics);
    }

    @GetMapping("/teacher/course/students/weekly")
    public Result<List<Map<String, Object>>> getCourseStudentsWeeklyTime(
            @RequestParam("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId) {
        List<Map<String, Object>> result = progressService.getCourseStudentsWeeklyTime(courseId, teacherId);
        return Result.success(result);
    }

    @GetMapping("/teacher/courses/weekly")
    public Result<List<Map<String, Object>>> getTeacherCoursesWeeklyTime(
            @RequestParam("teacherId") Long teacherId) {
        List<Map<String, Object>> result = progressService.getTeacherCoursesWeeklyTime(teacherId);
        return Result.success(result);
    }

    @GetMapping("/student/recent/weeks")
    public Result<List<Map<String, Object>>> getStudentRecentWeeksTime(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "weeks", required = false, defaultValue = "4") Integer weeks) {
        List<Map<String, Object>> result = progressService.getStudentRecentWeeksTime(studentId, weeks);
        return Result.success(result);
    }

    @GetMapping("/time-distribution")
    public Result<Map<String, Object>> getTimeDistribution(
            @RequestParam(value = "range", defaultValue = "7d") String range) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        int days = range.equals("30d") ? 30 : 7;
        List<Map<String, Object>> weeklyData = progressService.getStudentRecentWeeksTime(studentId, days);

        Map<String, Object> result = new java.util.HashMap<>();
        java.util.List<String> dayLabels = new java.util.ArrayList<>();
        java.util.List<Double> videoTimes = new java.util.ArrayList<>();
        java.util.List<Double> docTimes = new java.util.ArrayList<>();

        for (int i = 0; i < days; i++) {
            java.time.LocalDate date = java.time.LocalDate.now().minusDays(days - 1 - i);
            dayLabels.add(String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth()));
            videoTimes.add(0.0);
            docTimes.add(0.0);
        }

        result.put("days", dayLabels);
        result.put("video", videoTimes);
        result.put("doc", docTimes);
        return Result.success(result);
    }

    private Long getCurrentUserId() {
        com.ccut.context.UserContext.Context context = com.ccut.context.UserContext.get();
        return context == null ? null : context.userId();
    }
}
