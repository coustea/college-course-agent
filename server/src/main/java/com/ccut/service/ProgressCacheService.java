package com.ccut.service;

import com.ccut.dto.ProgressCacheItem;

import java.util.List;
import java.util.Map;

/**
 * 学习进度缓存服务接口
 * 使用Redis缓存进度数据，定时批量写入MySQL
 */
public interface ProgressCacheService {

    /**
     * 缓存进度数据到Redis
     * @param item 进度缓存项
     */
    void cacheProgress(ProgressCacheItem item);

    /**
     * 批量获取所有待处理的进度缓存
     * @return 按学生ID分组的进度数据列表
     */
    Map<Long, List<ProgressCacheItem>> getAllCachedProgress();

    /**
     * 删除已处理的缓存数据
     * @param studentId 学生ID
     * @param cacheKeys 缓存键列表
     */
    void removeCachedProgress(Long studentId, List<String> cacheKeys);

    /**
     * 获取指定学生的缓存数据量
     * @param studentId 学生ID
     * @return 缓存项数量
     */
    Long getCachedProgressCount(Long studentId);

    /**
     * 清空所有缓存数据（谨慎使用）
     */
    void clearAllCache();
}
