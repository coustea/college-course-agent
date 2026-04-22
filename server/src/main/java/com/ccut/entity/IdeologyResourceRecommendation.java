package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdeologyResourceRecommendation {
    private Long id;// 推荐ID
    private Long studentId;// 学生ID
    private Long resourceId;// 思政资源ID
    private Long courseId;// 课程ID
    private String reason;// 推荐理由
    private Double score;// 推荐分数
    private String recommendationType;// 推荐类型：TAG_MATCH/PROGRESS_SCENE/POPULAR
    private Boolean hasClicked;// 是否点击
    private LocalDateTime createdAt;// 创建时间
    private LocalDateTime clickedAt;// 点击时间
    private IdeologyResource resource;// 推荐资源详情
}
