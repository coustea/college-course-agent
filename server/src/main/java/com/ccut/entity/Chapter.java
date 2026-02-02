package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 课程章节实体
 * 支持树形结构：章 -> 节
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {
    private Long chapterId;          // 章节ID
    private Long courseId;           // 所属课程ID
    private Long parentId;           // 父章节ID（NULL表示一级章，非NULL表示小节）
    private String title;            // 章节标题
    private Integer chapterIndex;    // 章节序号（同级排序）
    private String chapterType;      // 类型：chapter(章) / section(节)
    private Integer contentType;     // 内容类型：0(无内容) 1(视频) 2(文档)
    private Date createTime;         // 创建时间
    private Date updateTime;         // 更新时间
    private Integer sort;            // 排序权重（数字越大越靠前）

    // 子章节列表（小节）
    private List<Chapter> children;

    // 该章节关联的内容（视频或文档）
    private CourseVideo video;
    private CourseDocument document;
}
