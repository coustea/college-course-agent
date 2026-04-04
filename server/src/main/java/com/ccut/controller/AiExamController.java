package com.ccut.controller;

import com.ccut.dto.AiExamGenerateRequest;
import com.ccut.dto.AiExamSubmitRequest;
import com.ccut.dto.Result;
import com.ccut.entity.AiExam;
import com.ccut.service.AiExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 考试控制器
 */
@RestController
@RequestMapping("/api/aiexam")
public class AiExamController {

    @Autowired
    private AiExamService aiExamService;

    /**
     * 生成 AI 考试
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody AiExamGenerateRequest req) {
        Map<String, Object> result = aiExamService.generateExam(req);
        return Result.success(result);
    }

    /**
     * 提交考试答案
     */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody AiExamSubmitRequest req) {
        Map<String, Object> result = aiExamService.submitExam(req);
        return Result.success(result);
    }

    /**
     * 查询某学生在某课程的题目正确率
     */
    @GetMapping("/accuracy")
    public Result<Map<String, Object>> accuracy(@RequestParam("studentId") Long studentId,
                                                @RequestParam("courseId") Long courseId) {
        Map<String, Object> result = aiExamService.getAccuracy(studentId, courseId);
        return Result.success(result);
    }

    /**
     * 获取学生在某课程中题目数为 5 的考试的平均成绩
     */
    @GetMapping("/average-score")
    public Result<Map<String, Object>> getAverageScore(@RequestParam("studentId") Long studentId,
                                                       @RequestParam("courseId") Long courseId) {
        Map<String, Object> result = aiExamService.getAverageScore(studentId, courseId);
        return Result.success(result);
    }

    /**
     * 获取学生在某课程中每个视频和文档的详细成绩
     */
    @GetMapping("/detailed-scores")
    public Result<Map<String, Object>> getDetailedScores(@RequestParam("studentId") Long studentId,
                                                          @RequestParam("courseId") Long courseId) {
        Map<String, Object> result = aiExamService.getDetailedScores(studentId, courseId);
        return Result.success(result);
    }

    /**
     * 获取学生在某课程中的所有考试记录
     */
    @GetMapping("/list")
    public Result<List<AiExam>> listExams(@RequestParam("studentId") Long studentId,
                                          @RequestParam("courseId") Long courseId) {
        List<AiExam> exams = aiExamService.listExams(studentId, courseId);
        return Result.success(exams);
    }
}
