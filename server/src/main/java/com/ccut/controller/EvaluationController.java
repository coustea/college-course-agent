package com.ccut.controller;

import com.ccut.dto.BehaviorAnalysisResult;
import com.ccut.dto.CourseEvaluationSummary;
import com.ccut.dto.EvaluationResult;
import com.ccut.dto.Result;
import com.ccut.entity.LearningPathRecord;
import com.ccut.entity.StudentBehaviorProfile;
import com.ccut.service.BehaviorAnalysisService;
import com.ccut.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教学评价与分析控制器
 * 提供教学评价、行为分析等API
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private BehaviorAnalysisService behaviorAnalysisService;

    // ==================== 教学评价相关 ====================

    /**
     * 计算学生评价
     */
    @PostMapping("/student/{studentId}/course/{courseId}")
    public Result<EvaluationResult> evaluateStudent(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            @RequestParam(value = "period", required = false) String period) {
        return Result.success(evaluationService.evaluateStudent(studentId, courseId, period));
    }

    /**
     * 批量计算课程所有学生评价
     */
    @PostMapping("/course/{courseId}/batch")
    public Result<List<EvaluationResult>> evaluateCourseStudents(
            @PathVariable Long courseId,
            @RequestParam(value = "period", required = false) String period) {
        return Result.success(evaluationService.evaluateCourseStudents(courseId, period));
    }

    /**
     * 获取学生评价历史
     */
    @GetMapping("/student/{studentId}/history")
    public Result<List<EvaluationResult>> getStudentEvaluationHistory(
            @PathVariable Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        return Result.success(evaluationService.getStudentEvaluationHistory(studentId, courseId));
    }

    /**
     * 获取课程评价汇总
     */
    @GetMapping("/course/{courseId}/summary")
    public Result<CourseEvaluationSummary> getCourseSummary(
            @PathVariable Long courseId,
            @RequestParam(value = "period", required = false) String period) {
        return Result.success(evaluationService.getCourseSummary(courseId, period));
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/{evaluationId}")
    public Result<EvaluationResult> getEvaluationDetail(@PathVariable Long evaluationId) {
        EvaluationResult result = evaluationService.getEvaluationDetail(evaluationId);
        return result != null ? Result.success(result) : Result.error(404, "评价记录不存在");
    }

    /**
     * 获取课程预警学生
     */
    @GetMapping("/course/{courseId}/warnings")
    public Result<List<CourseEvaluationSummary.StudentWarning>> getWarningStudents(@PathVariable Long courseId) {
        return Result.success(evaluationService.getWarningStudents(courseId));
    }

    // ==================== 行为分析相关 ====================

    /**
     * 分析学生行为特征
     */
    @GetMapping("/behavior/student/{studentId}")
    public Result<BehaviorAnalysisResult> analyzeStudentBehavior(@PathVariable Long studentId) {
        return Result.success(behaviorAnalysisService.analyzeStudentBehavior(studentId));
    }

    /**
     * 获取学生学习路径
     */
    @GetMapping("/behavior/student/{studentId}/path")
    public Result<List<LearningPathRecord>> getLearningPath(
            @PathVariable Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(behaviorAnalysisService.getLearningPath(studentId, courseId, limit));
    }

    /**
     * 记录学习行为
     */
    @PostMapping("/behavior/record")
    public Result<String> recordLearningAction(@RequestBody LearningActionRequest request) {
        behaviorAnalysisService.recordLearningAction(
                request.getStudentId(),
                request.getCourseId(),
                request.getResourceType(),
                request.getResourceId(),
                request.getResourceTitle(),
                request.getActionType(),
                request.getDurationSeconds(),
                request.getProgressPercent()
        );
        return Result.success("记录成功");
    }

    /**
     * 获取学生行为画像
     */
    @GetMapping("/behavior/student/{studentId}/profile")
    public Result<StudentBehaviorProfile> getStudentProfile(@PathVariable Long studentId) {
        StudentBehaviorProfile profile = behaviorAnalysisService.getStudentProfile(studentId);
        return profile != null ? Result.success(profile) : Result.error(404, "画像不存在");
    }

    /**
     * 更新学生行为画像
     */
    @PostMapping("/behavior/student/{studentId}/refresh")
    public Result<StudentBehaviorProfile> refreshProfile(@PathVariable Long studentId) {
        return Result.success(behaviorAnalysisService.updateBehaviorProfile(studentId));
    }

    /**
     * 计算行为有效性评分
     */
    @GetMapping("/behavior/student/{studentId}/effectiveness")
    public Result<Double> getBehaviorEffectiveness(
            @PathVariable Long studentId,
            @RequestParam Long courseId) {
        return Result.success(behaviorAnalysisService.calculateBehaviorEffectiveness(studentId, courseId));
    }

    /**
     * 学习行为请求DTO
     */
    @lombok.Data
    public static class LearningActionRequest {
        private Long studentId;
        private Long courseId;
        private String resourceType;
        private Long resourceId;
        private String resourceTitle;
        private String actionType;
        private Integer durationSeconds;
        private Double progressPercent;
    }
}