package com.ccut.service;

import com.ccut.dto.CourseEvaluationSummary;
import com.ccut.dto.EvaluationResult;
import com.ccut.entity.TeachingEvaluation;

import java.util.List;

/**
 * 教学评价服务接口
 * 多维度教学评价系统
 */
public interface EvaluationService {

    /**
     * 计算学生课程评价
     * 综合学习进度、思政资源使用、互动数据等计算四维度评分
     */
    EvaluationResult evaluateStudent(Long studentId, Long courseId, String period);

    /**
     * 批量计算课程所有学生评价
     */
    List<EvaluationResult> evaluateCourseStudents(Long courseId, String period);

    /**
     * 获取学生评价历史
     */
    List<EvaluationResult> getStudentEvaluationHistory(Long studentId, Long courseId);

    /**
     * 获取课程评价汇总
     */
    CourseEvaluationSummary getCourseSummary(Long courseId, String period);

    /**
     * 获取评价详情
     */
    EvaluationResult getEvaluationDetail(Long evaluationId);

    /**
     * 保存评价记录
     */
    TeachingEvaluation saveEvaluation(TeachingEvaluation evaluation);

    /**
     * 计算内容融入度评分
     * 基于课程关联思政资源数量、覆盖率等
     */
    Double calculateContentIntegrationScore(Long courseId, Long studentId);

    /**
     * 计算师生互动评分
     * 基于AI对话次数、作业反馈次数等
     */
    Double calculateInteractionScore(Long studentId, Long courseId);

    /**
     * 计算学生参与度评分
     * 基于学习时长、视频完成率、文档阅读率等
     */
    Double calculateParticipationScore(Long studentId, Long courseId);

    /**
     * 计算价值认同评分
     * 基于思政资源点击率、学习反思提交等
     */
    Double calculateValueRecognitionScore(Long studentId, Long courseId);

    /**
     * 获取需要预警的学生列表
     */
    List<CourseEvaluationSummary.StudentWarning> getWarningStudents(Long courseId);
}