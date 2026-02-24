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
 * AI 考试控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/aiexam")
public class AiExamController {

    @Autowired
    private AiExamService aiExamService;

    /**
     * 生成 AI 考试
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody AiExamGenerateRequest req) {
        log.debug("收到生成 AI 考试请求：URI=/api/aiexam/generate, 参数：req={}", req);
        try {
            log.info("执行生成 AI 考试业务：studentId={}, courseId={}", req.studentId(), req.courseId());
            Map<String, Object> result = aiExamService.generateExam(req);
            log.debug("生成 AI 考试成功：studentId={}, courseId={}", req.studentId(), req.courseId());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("生成考试参数错误：studentId={}, courseId={}, 错误：{}", req.studentId(), req.courseId(), e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("生成考试业务异常：studentId={}, courseId={}, 错误：{}", req.studentId(), req.courseId(), e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成考试异常：studentId={}, courseId={}, 错误：{}", req.studentId(), req.courseId(), e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 提交考试答案
     */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody AiExamSubmitRequest req) {
        log.debug("收到提交 AI 考试请求：URI=/api/aiexam/submit, 参数：req={}", req);
        try {
            log.info("执行提交 AI 考试业务：examId={}, studentId={}", req.examId(), req.studentId());
            Map<String, Object> result = aiExamService.submitExam(req);
            log.debug("提交 AI 考试成功：examId={}, studentId={}", req.examId(), req.studentId());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("提交考试参数错误：examId={}, studentId={}, 错误：{}", req.examId(), req.studentId(), e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("提交考试异常：examId={}, studentId={}, 错误：{}", req.examId(), req.studentId(), e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询某学生在某课程的题目正确率
     */
    @GetMapping("/accuracy")
    public Result<Map<String, Object>> accuracy(@RequestParam("studentId") Long studentId,
                                                @RequestParam("courseId") Long courseId) {
        log.debug("收到查询正确率请求：URI=/api/aiexam/accuracy, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询正确率业务：studentId={}, courseId={}", studentId, courseId);
            Map<String, Object> result = aiExamService.getAccuracy(studentId, courseId);
            log.debug("查询正确率成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("查询正确率参数错误：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询正确率异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生在某课程中题目数为 5 的考试的平均成绩
     */
    @GetMapping("/average-score")
    public Result<Map<String, Object>> getAverageScore(@RequestParam("studentId") Long studentId,
                                                       @RequestParam("courseId") Long courseId) {
        log.debug("收到查询平均成绩请求：URI=/api/aiexam/average-score, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询平均成绩业务：studentId={}, courseId={}", studentId, courseId);
            Map<String, Object> result = aiExamService.getAverageScore(studentId, courseId);
            log.debug("查询平均成绩成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("查询平均成绩参数错误：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询平均成绩异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生在某课程中每个视频和文档的详细成绩
     */
    @GetMapping("/detailed-scores")
    public Result<Map<String, Object>> getDetailedScores(@RequestParam("studentId") Long studentId,
                                                          @RequestParam("courseId") Long courseId) {
        log.debug("收到查询详细成绩请求：URI=/api/aiexam/detailed-scores, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询详细成绩业务：studentId={}, courseId={}", studentId, courseId);
            Map<String, Object> result = aiExamService.getDetailedScores(studentId, courseId);
            log.debug("查询详细成绩成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("查询详细成绩参数错误：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询详细成绩异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取学生在某课程中的所有考试记录
     */
    @GetMapping("/list")
    public Result<List<AiExam>> listExams(@RequestParam("studentId") Long studentId,
                                          @RequestParam("courseId") Long courseId) {
        log.debug("收到查询考试列表请求：URI=/api/aiexam/list, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行查询考试列表业务：studentId={}, courseId={}", studentId, courseId);
            List<AiExam> exams = aiExamService.listExams(studentId, courseId);
            log.debug("查询考试列表成功：studentId={}, courseId={}, 结果数={}", studentId, courseId, exams.size());
            return Result.success(exams);
        } catch (IllegalArgumentException e) {
            log.warn("查询考试列表参数错误：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询考试列表异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
