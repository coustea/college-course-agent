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
 * 每隔 5 分钟将 Redis 中的进度数据批量写入 MySQL
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
     * 定时刷新进度数据到 MySQL
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 分钟
    public void flushProgressToMySQL() {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始批量刷新进度数据到 MySQL (定时任务) ==========");
        log.debug("定时任务触发时间：{}", new Date(startTime));

        try {
            // 1. 获取所有待处理的缓存数据
            Map<Long, List<ProgressCacheItem>> allProgress = progressCacheService.getAllCachedProgress();

            if (allProgress.isEmpty()) {
                log.debug("没有待处理的进度数据，跳过本次执行");
                return;
            }

            int totalStudents = allProgress.size();
            int totalItems = allProgress.values().stream().mapToInt(List::size).sum();
            log.info("待处理数据：{}个学生，{}条进度记录", totalStudents, totalItems);
            log.debug("学生 ID 列表：{}", allProgress.keySet());

            // 2. 按学生逐个处理
            int successCount = 0;
            int failedStudents = 0;
            int totalProcessedItems = 0;

            for (Map.Entry<Long, List<ProgressCacheItem>> entry : allProgress.entrySet()) {
                Long studentId = entry.getKey();
                List<ProgressCacheItem> items = entry.getValue();
                long studentStartTime = System.currentTimeMillis();

                try {
                    log.debug("开始处理学生{}的进度数据，共{}条记录", studentId, items.size());

                    // 处理该学生的所有进度数据
                    processStudentProgress(studentId, items);
                    successCount++;
                    totalProcessedItems += items.size();

                    // 处理成功后删除缓存
                    List<String> cacheKeys = items.stream()
                        .map(this::buildCacheKey)
                        .toList();
                    progressCacheService.removeCachedProgress(studentId, cacheKeys);

                    long studentDuration = System.currentTimeMillis() - studentStartTime;
                    log.debug("学生{}的进度处理完成，耗时：{}ms", studentId, studentDuration);

                } catch (Exception e) {
                    failedStudents++;
                    log.error("处理学生{}的进度数据失败：error={}", studentId, e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("========== 批量刷新完成 ==========");
            log.info("统计：成功学生数={}/{}, 失败学生数={}, 处理进度条数={}, 总耗时={}ms",
                successCount, totalStudents, failedStudents, totalProcessedItems, duration);
            log.info("性能指标：平均每学生耗时={}ms, 平均每条进度耗时={}ms",
                totalStudents > 0 ? duration / totalStudents : 0,
                totalProcessedItems > 0 ? duration / totalProcessedItems : 0);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("批量刷新进度数据异常：error={}, 耗时：{}ms", e.getMessage(), duration, e);
        }
    }

    /**
     * 处理单个学生的所有进度数据
     */
    private void processStudentProgress(Long studentId, List<ProgressCacheItem> items) {
        log.debug("处理学生{}的{}条进度数据", studentId, items.size());

        // 统计每个课程的累计学习时长
        Map<String, CourseProgressSummary> summaryMap = new java.util.HashMap<>();

        // 按资源 ID 合并重复上报
        Map<String, ProgressCacheItem> mergedItems = new java.util.HashMap<>();

        for (ProgressCacheItem item : items) {
            String key = buildItemKey(item);

            // 合并相同资源的多次上报
            if (mergedItems.containsKey(key)) {
                ProgressCacheItem existing = mergedItems.get(key);
                existing.setDeltaSec(existing.getDeltaSec() + item.getDeltaSec());
                existing.setCompleted(existing.getCompleted() || item.getCompleted());
                log.trace("合并学生{}的课程{}资源{}的进度上报", studentId, item.getCourseId(), key);
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

        log.debug("学生{}的进度数据合并完成：原始{}条 -> 合并后{}条", studentId, items.size(), mergedItems.size());

        // 批量写入数据库
        int videoCount = 0;
        int documentCount = 0;

        for (ProgressCacheItem item : mergedItems.values()) {
            try {
                // 写入 video_progress 或 document_progress
                if (item.getVideoId() != null) {
                    videoProgressMapper.upsert(
                        item.getStudentId(),
                        item.getCourseId(),
                        item.getVideoId(),
                        item.getDeltaSec(),
                        item.getCompleted()
                    );
                    videoCount++;
                } else {
                    documentProgressMapper.upsert(
                        item.getStudentId(),
                        item.getCourseId(),
                        item.getDocumentId(),
                        item.getDeltaSec(),
                        item.getScrollPct(),
                        item.getCompleted()
                    );
                    documentCount++;
                }
            } catch (Exception e) {
                log.error("写入进度失败：studentId={}, courseId={}, resourceId={}", 
                    studentId, item.getCourseId(), 
                    item.getVideoId() != null ? item.getVideoId() : item.getDocumentId(), e);
            }
        }

        // 更新课程进度和每周学习时间
        int courseUpdateCount = 0;
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
                courseUpdateCount++;
            } catch (Exception e) {
                log.error("更新课程进度失败：studentId={}, courseId={}",
                    summary.studentId, summary.courseId, e);
            }
        }

        log.debug("学生{}的进度已刷新到 MySQL: 视频{}条，文档{}条，课程更新{}个", 
            studentId, videoCount, documentCount, courseUpdateCount);
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
