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
        log.debug("收到进度上报请求：URI=/api/progress/report, 参数：studentId={}, courseId={}, videoId={}, documentId={}, deltaSec={}, scrollPct={}, completed={}",
                studentId, courseId, videoId, documentId, deltaSec, scrollPct, completed);
        try {
            log.info("执行进度上报业务：studentId={}, courseId={}, videoId={}, documentId={}, deltaSec={}",
                    studentId, courseId, videoId, documentId, deltaSec);
            progressService.reportProgress(studentId, courseId, videoId, documentId, deltaSec, scrollPct, completed);
            log.debug("进度上报成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success("ok");
        } catch (IllegalArgumentException e) {
            log.warn("进度上报参数错误：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("进度上报异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询课程汇总进度
     */
    @GetMapping("/course")
    public Result<LearningProgress> getCourseProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId) {
        log.debug("收到查询课程进度请求：URI=/api/progress/course, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询课程进度业务：studentId={}, courseId={}", studentId, courseId);
            LearningProgress progress = progressService.getCourseProgress(studentId, courseId);
            log.debug("查询课程进度成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("查询课程进度异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
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
        log.debug("收到查询视频进度请求：URI=/api/progress/video, 参数：studentId={}, courseId={}, videoId={}", studentId, courseId, videoId);
        try {
            log.info("执行查询视频进度业务：studentId={}, courseId={}, videoId={}", studentId, courseId, videoId);
            VideoProgress progress = progressService.getVideoProgress(studentId, courseId, videoId);
            log.debug("查询视频进度成功：studentId={}, courseId={}, videoId={}", studentId, courseId, videoId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("查询视频进度异常：studentId={}, courseId={}, videoId={}, 错误：{}", studentId, courseId, videoId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出一个课程下该生所有视频的进度
     */
    @GetMapping("/video/list")
    public Result<List<VideoProgress>> listVideoProgress(@RequestParam("studentId") Long studentId,
                                                         @RequestParam("courseId") Long courseId) {
        log.debug("收到查询视频进度列表请求：URI=/api/progress/video/list, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询视频进度列表业务：studentId={}, courseId={}", studentId, courseId);
            List<VideoProgress> progresses = progressService.listVideoProgress(studentId, courseId);
            log.debug("查询视频进度列表成功：studentId={}, courseId={}, 视频数={}", studentId, courseId, progresses.size());
            return Result.success(progresses);
        } catch (Exception e) {
            log.error("查询视频进度列表异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
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
        log.debug("收到查询文档进度请求：URI=/api/progress/document, 参数：studentId={}, courseId={}, documentId={}", studentId, courseId, documentId);
        try {
            log.info("执行查询文档进度业务：studentId={}, courseId={}, documentId={}", studentId, courseId, documentId);
            DocumentProgress progress = progressService.getDocumentProgress(studentId, courseId, documentId);
            log.debug("查询文档进度成功：studentId={}, courseId={}, documentId={}", studentId, courseId, documentId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("查询文档进度异常：studentId={}, courseId={}, documentId={}, 错误：{}", studentId, courseId, documentId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出一个课程下该生所有文档的进度
     */
    @GetMapping("/document/list")
    public Result<List<DocumentProgress>> listDocumentProgress(@RequestParam("studentId") Long studentId,
                                                               @RequestParam("courseId") Long courseId) {
        log.debug("收到查询文档进度列表请求：URI=/api/progress/document/list, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询文档进度列表业务：studentId={}, courseId={}", studentId, courseId);
            List<DocumentProgress> progresses = progressService.listDocumentProgress(studentId, courseId);
            log.debug("查询文档进度列表成功：studentId={}, courseId={}, 文档数={}", studentId, courseId, progresses.size());
            return Result.success(progresses);
        } catch (Exception e) {
            log.error("查询文档进度列表异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 聚合查询：课程下该生所有视频与文档的进度 + 汇总（未学的节也以 0% 返回）
     */
    @GetMapping("/course/all")
    public Result<Map<String, Object>> getAllProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId) {
        log.debug("收到查询全部进度请求：URI=/api/progress/course/all, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询全部进度业务：studentId={}, courseId={}", studentId, courseId);
            Map<String, Object> progress = progressService.getAllProgress(studentId, courseId);
            log.debug("查询全部进度成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("查询全部进度异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生统计数据
     */
    @GetMapping("/student/statistics")
    public Result<com.ccut.dto.StudentStatistics> getStudentStatistics(@RequestParam("studentId") Long studentId) {
        log.debug("收到查询学生统计数据请求：URI=/api/progress/student/statistics, 参数：studentId={}", studentId);
        try {
            log.info("执行查询学生统计数据业务：studentId={}", studentId);
            com.ccut.dto.StudentStatistics statistics = progressService.getStudentStatistics(studentId);
            log.debug("查询学生统计数据成功：studentId={}", studentId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("查询学生统计数据异常：studentId={}, 错误：{}", studentId, e.getMessage(), e);
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
        log.debug("收到查询课程学生本周学习时间请求：URI=/api/progress/teacher/course/students/weekly, 参数：courseId={}, teacherId={}", courseId, teacherId);
        try {
            log.info("执行查询课程学生本周学习时间业务：courseId={}, teacherId={}", courseId, teacherId);
            List<Map<String, Object>> result = progressService.getCourseStudentsWeeklyTime(courseId, teacherId);
            log.debug("查询课程学生本周学习时间成功：courseId={}, teacherId={}, 学生数={}", courseId, teacherId, result.size());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("权限验证失败：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("查询课程学生本周学习时间异常：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取教师所有课程的学生本周学习情况汇总
     */
    @GetMapping("/teacher/courses/weekly")
    public Result<List<Map<String, Object>>> getTeacherCoursesWeeklyTime(
            @RequestParam("teacherId") Long teacherId) {
        log.debug("收到查询教师课程本周学习时间请求：URI=/api/progress/teacher/courses/weekly, 参数：teacherId={}", teacherId);
        try {
            log.info("执行查询教师课程本周学习时间业务：teacherId={}", teacherId);
            List<Map<String, Object>> result = progressService.getTeacherCoursesWeeklyTime(teacherId);
            log.debug("查询教师课程本周学习时间成功：teacherId={}, 课程数={}", teacherId, result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询教师课程本周学习时间异常：teacherId={}, 错误：{}", teacherId, e.getMessage(), e);
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
        log.debug("收到查询学生最近周学习时间请求：URI=/api/progress/student/recent/weeks, 参数：studentId={}, weeks={}", studentId, weeks);
        try {
            log.info("执行查询学生最近周学习时间业务：studentId={}, weeks={}", studentId, weeks);
            List<Map<String, Object>> result = progressService.getStudentRecentWeeksTime(studentId, weeks);
            log.debug("查询学生最近周学习时间成功：studentId={}, weeks={}", studentId, weeks);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询学生最近周学习时间异常：studentId={}, weeks={}, 错误：{}", studentId, weeks, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
