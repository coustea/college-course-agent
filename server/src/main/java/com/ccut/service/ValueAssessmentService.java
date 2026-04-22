package com.ccut.service;

import com.ccut.dto.ValueAssessmentSummary;
import com.ccut.dto.ValueTrendPrediction;
import com.ccut.entity.TeachingStyleProfile;
import com.ccut.entity.ValueAssessment;

import java.util.List;

/**
 * 价值认同评估服务接口
 */
public interface ValueAssessmentService {

    /**
     * 评估学生价值认同
     */
    ValueAssessment assessStudentValue(Long studentId, Long courseId, String period);

    /**
     * 获取学生价值认同趋势
     */
    List<ValueAssessment> getValueTrend(Long studentId, Long courseId, int months);

    /**
     * 批量评估课程学生
     */
    List<ValueAssessment> batchAssessCourseStudents(Long courseId, String period);

    /**
     * 获取教师授课风格画像
     */
    TeachingStyleProfile getTeacherStyleProfile(Long teacherId);

    /**
     * 分析教师授课风格
     */
    TeachingStyleProfile analyzeTeachingStyle(Long teacherId);

    /**
     * 获取课程价值认同汇总
     */
    ValueAssessmentSummary getCourseValueSummary(Long courseId);

    /**
     * 预测价值认同趋势
     */
    ValueTrendPrediction predictValueTrend(Long studentId, Long courseId);
}