package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 学习进度缓存项
 * 用于Redis中暂存进度数据，定时批量写入MySQL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressCacheItem {
    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 视频ID（与documentId二选一）
     */
    private Long videoId;

    /**
     * 文档ID（与videoId二选一）
     */
    private Long documentId;

    /**
     * 学习时长增量（秒）
     */
    private Integer deltaSec;

    /**
     * 文档滚动百分比（仅文档有效）
     */
    private Double scrollPct;

    /**
     * 是否完成
     */
    private Boolean completed;

    /**
     * 上报时间戳
     */
    private Long timestamp;

    public ProgressCacheItem(Long studentId, Long courseId, Long videoId, Long documentId,
                            Integer deltaSec, Double scrollPct, Boolean completed) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.videoId = videoId;
        this.documentId = documentId;
        this.deltaSec = deltaSec != null ? deltaSec : 0;
        this.scrollPct = scrollPct != null ? scrollPct : 0.0;
        this.completed = completed != null ? completed : false;
        this.timestamp = System.currentTimeMillis();
    }
}
