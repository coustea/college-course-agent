package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdeologyResource {
    private Long resourceId;// 思政资源ID
    private Long courseId;// 关联课程ID
    private Long chapterId;// 关联章节ID
    private Long videoId;// 关联视频ID
    private Long documentId;// 关联文档ID
    private String title;// 资源标题
    private String resourceType;// 资源类型：case/policy/video/document/activity
    private String contentSummary;// 内容摘要
    private String sourceUrl;// 资源地址
    private String valueTheme;// 价值主题
    private String applicableScene;// 适用教学场景
    private String keywords;// 关键词，逗号分隔
    private String difficulty;// 难度：easy/medium/hard
    private String status;// 状态：draft/published
    private Long createdBy;// 创建人
    private LocalDateTime createdAt;// 创建时间
    private LocalDateTime updatedAt;// 更新时间
}
