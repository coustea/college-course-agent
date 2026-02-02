package com.ccut.service.Impl;

import com.ccut.dto.ProgressCacheItem;
import com.ccut.service.ProgressCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学习进度缓存服务实现
 * 使用Redis Hash结构存储：
 * - Key: "progress:cache:{studentId}"
 * - Field: "{courseId}:{videoId|documentId}:{timestamp}"
 * - Value: ProgressCacheItem (JSON)
 */
@Slf4j
@Service
public class ProgressCacheServiceImpl implements ProgressCacheService {

    private static final String CACHE_KEY_PREFIX = "progress:cache:";
    private static final long CACHE_EXPIRE_MINUTES = 10; // 缓存10分钟过期

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void cacheProgress(ProgressCacheItem item) {
        try {
            String key = buildCacheKey(item.getStudentId());
            String field = buildCacheField(item);

            // 存入Redis Hash
            redisTemplate.opsForHash().put(key, field, item);

            // 设置整个Hash的过期时间（每次写入都刷新）
            redisTemplate.expire(key, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            log.debug("进度数据已缓存: studentId={}, field={}", item.getStudentId(), field);

        } catch (Exception e) {
            // Redis失败不影响主流程，记录错误日志
            log.error("缓存进度数据失败: studentId={}, error={}",
                item.getStudentId(), e.getMessage(), e);
        }
    }

    @Override
    public Map<Long, List<ProgressCacheItem>> getAllCachedProgress() {
        Map<Long, List<ProgressCacheItem>> result = new HashMap<>();

        try {
            // 扫描所有进度缓存键
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.debug("没有待处理的进度缓存数据");
                return result;
            }

            log.info("发现{}个学生的缓存数据", keys.size());

            // 遍历每个学生的缓存
            for (String key : keys) {
                try {
                    // 提取studentId
                    Long studentId = extractStudentId(key);
                    if (studentId == null) continue;

                    // 获取该学生的所有缓存项
                    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

                    // 转换为ProgressCacheItem列表
                    List<ProgressCacheItem> items = entries.values().stream()
                        .filter(obj -> obj instanceof ProgressCacheItem)
                        .map(obj -> (ProgressCacheItem) obj)
                        .collect(Collectors.toList());

                    if (!items.isEmpty()) {
                        result.put(studentId, items);
                        log.debug("学生{}有{}条待处理缓存", studentId, items.size());
                    }

                } catch (Exception e) {
                    log.error("处理学生缓存失败: key={}, error={}", key, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("获取所有缓存数据失败: error={}", e.getMessage(), e);
        }

        return result;
    }

    @Override
    public void removeCachedProgress(Long studentId, List<String> cacheKeys) {
        try {
            String key = buildCacheKey(studentId);

            if (cacheKeys != null && !cacheKeys.isEmpty()) {
                // 删除指定的Hash字段
                redisTemplate.opsForHash().delete(key, cacheKeys.toArray());
                log.debug("已删除学生{}的{}条缓存", studentId, cacheKeys.size());
            } else {
                // 删除整个键
                redisTemplate.delete(key);
                log.debug("已删除学生{}的所有缓存", studentId);
            }

        } catch (Exception e) {
            log.error("删除缓存失败: studentId={}, error={}", studentId, e.getMessage(), e);
        }
    }

    @Override
    public Long getCachedProgressCount(Long studentId) {
        try {
            String key = buildCacheKey(studentId);
            return redisTemplate.opsForHash().size(key);
        } catch (Exception e) {
            log.error("获取缓存数量失败: studentId={}, error={}", studentId, e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public void clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.warn("已清空所有进度缓存，共{}个键", keys.size());
            }
        } catch (Exception e) {
            log.error("清空缓存失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(Long studentId) {
        return CACHE_KEY_PREFIX + studentId;
    }

    /**
     * 构建缓存字段
     * 格式: {courseId}:{type}:{itemId}:{timestamp}
     * 例如: 100:video:1:1706789123456
     */
    private String buildCacheField(ProgressCacheItem item) {
        String type = item.getVideoId() != null ? "video" : "document";
        Long itemId = item.getVideoId() != null ? item.getVideoId() : item.getDocumentId();
        return String.format("%d:%s:%d:%d",
            item.getCourseId(), type, itemId, item.getTimestamp());
    }

    /**
     * 从缓存键中提取学生ID
     */
    private Long extractStudentId(String key) {
        try {
            String suffix = key.substring(CACHE_KEY_PREFIX.length());
            return Long.parseLong(suffix);
        } catch (Exception e) {
            log.error("解析学生ID失败: key={}", key, e);
            return null;
        }
    }
}
