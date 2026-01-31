package com.ccut.controller;

import com.ccut.dto.AiExamGenerateRequest;
import com.ccut.dto.AiExamSubmitRequest;
import com.ccut.dto.Result;
import com.ccut.entity.AiExam;
import com.ccut.service.AiExamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI考试控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/aiexam")
public class AiExamController {

    @Autowired
    private AiExamService aiExamService;

    /**
     * 生成AI考试
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody AiExamGenerateRequest req) {
        try {
            return Result.success(aiExamService.generateExam(req));
        } catch (IllegalArgumentException e) {
            log.warn("生成考试参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("生成考试业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成考试异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 提交考试答案
     */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody AiExamSubmitRequest req) {
        try {
            return Result.success(aiExamService.submitExam(req));
        } catch (IllegalArgumentException e) {
            log.warn("提交考试参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("提交考试异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询某学生在某课程的题目正确率（0~1 小数）
     */
    @GetMapping("/accuracy")
    public Result<Map<String, Object>> accuracy(@RequestParam("studentId") Long studentId,
                                                @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(aiExamService.getAccuracy(studentId, courseId));
        } catch (IllegalArgumentException e) {
            log.warn("查询正确率参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询正确率异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生在某课程中题目数为5的考试的平均成绩
     */
    @GetMapping("/average-score")
    public Result<Map<String, Object>> getAverageScore(@RequestParam("studentId") Long studentId,
                                                       @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(aiExamService.getAverageScore(studentId, courseId));
        } catch (IllegalArgumentException e) {
            log.warn("查询平均成绩参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询平均成绩异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生在某课程中每个视频和文档的详细成绩
     */
    @GetMapping("/detailed-scores")
    public Result<Map<String, Object>> getDetailedScores(@RequestParam("studentId") Long studentId,
                                                          @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(aiExamService.getDetailedScores(studentId, courseId));
        } catch (IllegalArgumentException e) {
            log.warn("查询详细成绩参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询详细成绩异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生在某课程中的所有考试记录
     */
    @GetMapping("/list")
    public Result<List<AiExam>> listExams(@RequestParam("studentId") Long studentId,
                                          @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(aiExamService.listExams(studentId, courseId));
        } catch (IllegalArgumentException e) {
            log.warn("查询考试列表参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询考试列表异常", e);
            return Result.error(500, e.getMessage());
        }
    }

}
