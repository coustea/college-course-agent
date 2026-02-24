package com.ccut.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程推荐实体
 */
@Data
public class Recommendation {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String reason;          // 推荐理由
    private Double score;           // 推荐分数
    private String recommendationType; // 推荐类型：CONTENT_BASED, COLLABORATIVE, POPULAR
    private LocalDateTime createdAt;
    private Boolean hasClicked;     // 是否已点击
    private LocalDateTime clickedAt;
}
