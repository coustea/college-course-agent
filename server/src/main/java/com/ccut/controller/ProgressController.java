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

    /**
     * 上报学习增量：支持 videoId 或 documentId
     */
    @PostMapping("/report")
    public Result<String> report(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId,
                                 @RequestParam(value = "videoId", required = false) Long videoId,
                                 @RequestParam(value = "documentId", required = false) Long documentId,
                                 @RequestParam(value = "deltaSec", defaultValue = "0") Integer deltaSec,
                                 @RequestParam(value = "scrollPct", required = false) Double scrollPct,
                                 @RequestParam(value = "completed", defaultValue = "false") Boolean completed) {
        try {
            progressService.reportProgress(studentId, courseId, videoId, documentId, deltaSec, scrollPct, completed);
            return Result.success("ok");
        } catch (IllegalArgumentException e) {
            log.warn("进度上报参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("进度上报异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询课程汇总进度
     */
    @GetMapping("/course")
    public Result<LearningProgress> getCourseProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(progressService.getCourseProgress(studentId, courseId));
        } catch (Exception e) {
            log.error("查询课程进度异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询某个视频的学习进度
     */
    @GetMapping("/video")
    public Result<VideoProgress> getVideoProgress(@RequestParam("studentId") Long studentId,
                                                  @RequestParam("courseId") Long courseId,
                                                  @RequestParam("videoId") Long videoId) {
        try {
            return Result.success(progressService.getVideoProgress(studentId, courseId, videoId));
        } catch (Exception e) {
            log.error("查询视频进度异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出一个课程下该生所有视频的进度
     */
    @GetMapping("/video/list")
    public Result<List<VideoProgress>> listVideoProgress(@RequestParam("studentId") Long studentId,
                                                         @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(progressService.listVideoProgress(studentId, courseId));
        } catch (Exception e) {
            log.error("查询视频进度列表异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询某个文档的学习进度
     */
    @GetMapping("/document")
    public Result<DocumentProgress> getDocumentProgress(@RequestParam("studentId") Long studentId,
                                                        @RequestParam("courseId") Long courseId,
                                                        @RequestParam("documentId") Long documentId) {
        try {
            return Result.success(progressService.getDocumentProgress(studentId, courseId, documentId));
        } catch (Exception e) {
            log.error("查询文档进度异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出一个课程下该生所有文档的进度
     */
    @GetMapping("/document/list")
    public Result<List<DocumentProgress>> listDocumentProgress(@RequestParam("studentId") Long studentId,
                                                               @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(progressService.listDocumentProgress(studentId, courseId));
        } catch (Exception e) {
            log.error("查询文档进度列表异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 聚合查询：课程下该生所有视频与文档的进度 + 汇总（未学的节也以0%返回）
     */
    @GetMapping("/course/all")
    public Result<Map<String, Object>> getAllProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(progressService.getAllProgress(studentId, courseId));
        } catch (Exception e) {
            log.error("查询全部进度异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生统计数据
     */
    @GetMapping("/student/statistics")
    public Result<com.ccut.dto.StudentStatistics> getStudentStatistics(@RequestParam("studentId") Long studentId) {
        try {
            return Result.success(progressService.getStudentStatistics(studentId));
        } catch (Exception e) {
            log.error("查询学生统计数据异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取教师在指定课程的所有学生本周学习情况
     */
    @GetMapping("/teacher/course/students/weekly")
    public Result<List<Map<String, Object>>> getCourseStudentsWeeklyTime(
            @RequestParam("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId) {
        try {
            return Result.success(progressService.getCourseStudentsWeeklyTime(courseId, teacherId));
        } catch (IllegalArgumentException e) {
            log.warn("权限验证失败: {}", e.getMessage());
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("查询课程学生本周学习时间异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取教师所有课程的学生本周学习情况汇总
     */
    @GetMapping("/teacher/courses/weekly")
    public Result<List<Map<String, Object>>> getTeacherCoursesWeeklyTime(
            @RequestParam("teacherId") Long teacherId) {
        try {
            return Result.success(progressService.getTeacherCoursesWeeklyTime(teacherId));
        } catch (Exception e) {
            log.error("查询教师课程本周学习时间异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生最近几周的学习时间统计
     */
    @GetMapping("/student/recent/weeks")
    public Result<List<Map<String, Object>>> getStudentRecentWeeksTime(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "weeks", required = false, defaultValue = "4") Integer weeks) {
        try {
            return Result.success(progressService.getStudentRecentWeeksTime(studentId, weeks));
        } catch (Exception e) {
            log.error("查询学生最近周学习时间异常", e);
            return Result.error(500, e.getMessage());
        }
    }

}
