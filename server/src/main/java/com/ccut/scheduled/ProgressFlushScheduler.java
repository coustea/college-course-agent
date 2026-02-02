package com.ccut.scheduled;

import com.ccut.dto.ProgressCacheItem;
import com.ccut.mapper.DocumentProgressMapper;
import com.ccut.mapper.LearningProgressMapper;
import com.ccut.mapper.VideoProgressMapper;
import com.ccut.mapper.WeeklyStudyTimeMapper;
import com.ccut.service.ProgressCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 学习进度批量刷新定时任务
 * 每隔5分钟将Redis中的进度数据批量写入MySQL
 */
@Slf4j
@Component
public class ProgressFlushScheduler {

    @Autowired
    private ProgressCacheService progressCacheService;

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private DocumentProgressMapper documentProgressMapper;

    @Autowired
    private LearningProgressMapper learningProgressMapper;

    @Autowired
    private WeeklyStudyTimeMapper weeklyStudyTimeMapper;

    /**
     * 定时刷新进度数据到MySQL
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5分钟
    public void flushProgressToMySQL() {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始批量刷新进度数据到MySQL ==========");

        try {
            // 1. 获取所有待处理的缓存数据
            Map<Long, List<ProgressCacheItem>> allProgress = progressCacheService.getAllCachedProgress();

            if (allProgress.isEmpty()) {
                log.info("没有待处理的进度数据");
                return;
            }

            int totalStudents = allProgress.size();
            int totalItems = allProgress.values().stream().mapToInt(List::size).sum();
            log.info("待处理数据：{}个学生，{}条进度记录", totalStudents, totalItems);

            // 2. 按学生逐个处理
            int successCount = 0;
            int failedStudents = 0;

            for (Map.Entry<Long, List<ProgressCacheItem>> entry : allProgress.entrySet()) {
                Long studentId = entry.getKey();
                List<ProgressCacheItem> items = entry.getValue();

                try {
                    // 处理该学生的所有进度数据
                    processStudentProgress(studentId, items);
                    successCount++;

                    // 处理成功后删除缓存
                    List<String> cacheKeys = items.stream()
                        .map(this::buildCacheKey)
                        .toList();
                    progressCacheService.removeCachedProgress(studentId, cacheKeys);

                } catch (Exception e) {
                    failedStudents++;
                    log.error("处理学生{}的进度数据失败: error={}", studentId, e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("========== 批量刷新完成 ==========");
            log.info("成功: {}/{}, 失败: {}, 耗时: {}ms",
                successCount, totalStudents, failedStudents, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("批量刷新进度数据异常: error={}, 耗时: {}ms", e.getMessage(), duration, e);
        }
    }

    /**
     * 处理单个学生的所有进度数据
     */
    private void processStudentProgress(Long studentId, List<ProgressCacheItem> items) {
        // 统计每个课程的累计学习时长
        Map<String, CourseProgressSummary> summaryMap = new java.util.HashMap<>();

        // 按资源ID合并重复上报
        Map<String, ProgressCacheItem> mergedItems = new java.util.HashMap<>();

        for (ProgressCacheItem item : items) {
            String key = buildItemKey(item);

            // 合并相同资源的多次上报
            if (mergedItems.containsKey(key)) {
                ProgressCacheItem existing = mergedItems.get(key);
                existing.setDeltaSec(existing.getDeltaSec() + item.getDeltaSec());
                existing.setCompleted(existing.getCompleted() || item.getCompleted());
            } else {
                mergedItems.put(key, item);
            }

            // 统计课程总时长
            String courseKey = String.valueOf(item.getCourseId());
            CourseProgressSummary summary = summaryMap.getOrDefault(courseKey,
                new CourseProgressSummary(studentId, item.getCourseId()));
            summary.addDelta(item.getDeltaSec());
            summaryMap.put(courseKey, summary);
        }

        // 批量写入数据库
        for (ProgressCacheItem item : mergedItems.values()) {
            try {
                // 写入video_progress或document_progress
                if (item.getVideoId() != null) {
                    videoProgressMapper.upsert(
                        item.getStudentId(),
                        item.getCourseId(),
                        item.getVideoId(),
                        item.getDeltaSec(),
                        item.getCompleted()
                    );
                } else {
                    documentProgressMapper.upsert(
                        item.getStudentId(),
                        item.getCourseId(),
                        item.getDocumentId(),
                        item.getDeltaSec(),
                        item.getScrollPct(),
                        item.getCompleted()
                    );
                }
            } catch (Exception e) {
                log.error("写入进度失败: studentId={}, courseId={}", studentId, item.getCourseId(), e);
            }
        }

        // 更新课程进度和每周学习时间
        for (CourseProgressSummary summary : summaryMap.values()) {
            try {
                // 更新课程总进度
                learningProgressMapper.upsert(
                    summary.studentId,
                    summary.courseId,
                    0.0,
                    summary.totalDeltaSec,
                    false
                );

                // 如果有学习时长，更新每周学习时间
                if (summary.totalDeltaSec > 0) {
                    LocalDate today = LocalDate.now();
                    LocalDate monday = today.with(DayOfWeek.MONDAY);
                    Date weekStartDate = Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant());

                    weeklyStudyTimeMapper.upsert(
                        summary.studentId,
                        summary.courseId,
                        weekStartDate,
                        summary.totalDeltaSec
                    );

                    // 更新总体学习时间
                    weeklyStudyTimeMapper.upsert(
                        summary.studentId,
                        null,
                        weekStartDate,
                        summary.totalDeltaSec
                    );
                }
            } catch (Exception e) {
                log.error("更新课程进度失败: studentId={}, courseId={}",
                    summary.studentId, summary.courseId, e);
            }
        }

        log.debug("学生{}的{}条进度已刷新到MySQL", studentId, items.size());
    }

    /**
     * 构建缓存项的唯一标识
     */
    private String buildItemKey(ProgressCacheItem item) {
        String type = item.getVideoId() != null ? "video" : "document";
        Long itemId = item.getVideoId() != null ? item.getVideoId() : item.getDocumentId();
        return String.format("%d:%s:%d", item.getCourseId(), type, itemId);
    }

    /**
     * 构建缓存键（用于删除）
     */
    private String buildCacheKey(ProgressCacheItem item) {
        String type = item.getVideoId() != null ? "video" : "document";
        Long itemId = item.getVideoId() != null ? item.getVideoId() : item.getDocumentId();
        return String.format("%d:%s:%d:%d",
            item.getCourseId(), type, itemId, item.getTimestamp());
    }

    /**
     * 课程进度统计
     */
    private static class CourseProgressSummary {
        Long studentId;
        Long courseId;
        int totalDeltaSec;

        CourseProgressSummary(Long studentId, Long courseId) {
            this.studentId = studentId;
            this.courseId = courseId;
            this.totalDeltaSec = 0;
        }

        void addDelta(Integer delta) {
            if (delta != null) {
                this.totalDeltaSec += delta;
            }
        }
    }
}
