package com.ccut.service;

import com.ccut.dto.BehaviorAnalysisResult;
import com.ccut.entity.LearningPathRecord;
import com.ccut.entity.StudentBehaviorProfile;

import java.util.List;

/**
 * 行为分析服务接口
 * 学生学习行为画像分析
 */
public interface BehaviorAnalysisService {

    /**
     * 分析学生行为特征
     * 生成学生行为画像
     */
    BehaviorAnalysisResult analyzeStudentBehavior(Long studentId);

    /**
     * 分析课程所有学生行为
     */
    List<BehaviorAnalysisResult> analyzeCourseBehavior(Long courseId);

    /**
     * 获取学生学习路径
     */
    List<LearningPathRecord> getLearningPath(Long studentId, Long courseId, Integer limit);

    /**
     * 记录学习行为
     */
    void recordLearningAction(Long studentId, Long courseId, String resourceType,
                              Long resourceId, String resourceTitle, String actionType,
                              Integer durationSeconds, Double progressPercent);

    /**
     * 更新学生行为画像
     */
    StudentBehaviorProfile updateBehaviorProfile(Long studentId);

    /**
     * 计算行为有效性评分
     * 分析学习行为与学习成效的关联
     */
    Double calculateBehaviorEffectiveness(Long studentId, Long courseId);

    /**
     * 获取学生行为画像
     */
    StudentBehaviorProfile getStudentProfile(Long studentId);

    /**
     * 批量更新课程学生画像
     */
    void batchUpdateCourseProfiles(Long courseId);

    /**
     * 获取学习时段偏好分析
     */
    String analyzePreferredStudyTime(Long studentId);

    /**
     * 获取资源类型偏好分析
     */
    String analyzePreferredResourceType(Long studentId);
}