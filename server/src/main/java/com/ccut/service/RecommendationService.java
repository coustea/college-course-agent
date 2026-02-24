package com.ccut.service;

import com.ccut.entity.Recommendation;
import java.util.List;

/**
 * 推荐服务接口
 */
public interface RecommendationService {

    /**
     * 为学生生成课程推荐
     * @param studentId 学生ID
     * @param limit 推荐数量限制
     * @return 推荐列表
     */
    List<Recommendation> generateRecommendations(Long studentId, int limit);

    /**
     * 获取学生的推荐列表
     * @param studentId 学生ID
     * @param limit 数量限制
     * @return 推荐列表
     */
    List<Recommendation> getStudentRecommendations(Long studentId, int limit);

    /**
     * 标记推荐为已点击
     * @param recommendationId 推荐ID
     */
    void markRecommendationAsClicked(Long recommendationId);

    /**
     * 清理过期推荐
     * @param days 保留天数
     */
    void cleanupOldRecommendations(int days);

    /**
     * 刷新学生的推荐（删除旧的，生成新的）
     * @param studentId 学生ID
     */
    void refreshRecommendations(Long studentId);
}
