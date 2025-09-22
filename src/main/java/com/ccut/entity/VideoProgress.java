package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoProgress {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long videoId;
    private Integer watchedSeconds;
    private Boolean completed;
    private Date updatedAt;
    private Double percentage; // 计算字段：基于 watchedSeconds / 视频时长
}


