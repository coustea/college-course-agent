package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.dto.SentimentAnalysisResult;
import com.ccut.dto.SentimentDistribution;
import com.ccut.dto.ValueAssessmentSummary;
import com.ccut.dto.ValueTrendPrediction;
import com.ccut.entity.SentimentRecord;
import com.ccut.entity.TeachingStyleProfile;
import com.ccut.entity.ValueAssessment;
import com.ccut.service.SentimentAnalysisService;
import com.ccut.service.ValueAssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 情感分析与价值认同控制器
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private ValueAssessmentService valueAssessmentService;

    // ==================== 情感分析 ====================

    /**
     * 分析文本情感
     */
    @PostMapping("/sentiment")
    public Result<SentimentAnalysisResult> analyzeSentiment(@RequestBody Map<String, String> request) {
        return Result.success(sentimentAnalysisService.analyzeSentiment(request.get("text")));
    }

    /**
     * 获取学生情感趋势
     */
    @GetMapping("/sentiment/student/{studentId}")
    public Result<List<SentimentRecord>> getStudentSentimentTrend(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(sentimentAnalysisService.getStudentSentimentTrend(studentId, courseId, days));
    }

    /**
     * 获取课程情感分布
     */
    @GetMapping("/sentiment/course/{courseId}/distribution")
    public Result<SentimentDistribution> getCourseSentimentDistribution(
            @PathVariable Long courseId) {
        return Result.success(sentimentAnalysisService.getCourseSentimentDistribution(courseId));
    }

    /**
     * 分析思政情感
     */
    @PostMapping("/sentiment/ideology")
    public Result<SentimentAnalysisResult> analyzeIdeologySentiment(@RequestBody Map<String, String> request) {
        return Result.success(sentimentAnalysisService.analyzeIdeologySentiment(
                request.get("text"), request.get("theme")));
    }

    // ==================== 价值认同评估 ====================

    /**
     * 评估学生价值认同
     */
    @PostMapping("/value/student/{studentId}")
    public Result<ValueAssessment> assessStudentValue(
            @PathVariable Long studentId,
            @RequestParam Long courseId,
            @RequestParam(required = false) String period) {
        return Result.success(valueAssessmentService.assessStudentValue(studentId, courseId, period));
    }

    /**
     * 获取学生价值认同趋势
     */
    @GetMapping("/value/student/{studentId}/trend")
    public Result<List<ValueAssessment>> getValueTrend(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "6") int months) {
        return Result.success(valueAssessmentService.getValueTrend(studentId, courseId, months));
    }

    /**
     * 批量评估课程学生
     */
    @PostMapping("/value/course/{courseId}/batch")
    public Result<List<ValueAssessment>> batchAssessCourseStudents(
            @PathVariable Long courseId,
            @RequestParam(required = false) String period) {
        return Result.success(valueAssessmentService.batchAssessCourseStudents(courseId, period));
    }

    /**
     * 获取课程价值认同汇总
     */
    @GetMapping("/value/course/{courseId}/summary")
    public Result<ValueAssessmentSummary> getCourseValueSummary(
            @PathVariable Long courseId) {
        return Result.success(valueAssessmentService.getCourseValueSummary(courseId));
    }

    /**
     * 预测价值认同趋势
     */
    @GetMapping("/value/student/{studentId}/predict")
    public Result<ValueTrendPrediction> predictValueTrend(
            @PathVariable Long studentId,
            @RequestParam Long courseId) {
        return Result.success(valueAssessmentService.predictValueTrend(studentId, courseId));
    }

    // ==================== 教师授课风格 ====================

    /**
     * 获取教师授课风格画像
     */
    @GetMapping("/style/teacher/{teacherId}")
    public Result<TeachingStyleProfile> getTeacherStyleProfile(@PathVariable Long teacherId) {
        return Result.success(valueAssessmentService.getTeacherStyleProfile(teacherId));
    }

    /**
     * 分析教师授课风格
     */
    @PostMapping("/style/teacher/{teacherId}/analyze")
    public Result<TeachingStyleProfile> analyzeTeachingStyle(@PathVariable Long teacherId) {
        return Result.success(valueAssessmentService.analyzeTeachingStyle(teacherId));
    }
}
