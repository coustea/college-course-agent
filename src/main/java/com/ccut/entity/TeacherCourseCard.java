package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseCard {
    private Long id;                 // course_id
    private String title;            // course_name
    private String description;      // description
    private String image;            // 兼容旧字段，后端同样回传 resource_url
    private String resourceUrl;      // 直接回传数据库 resource_url，供前端优先使用
    private String status;           // 草稿/发布（无字段时默认 published）
    private Long categoryId;         // 分类（当前表无，默认 null）
    private Date createTime;         // 使用 start_date 占位

    private Integer studentCount;    // 选课学生数（active）
    private Integer videoCount;      // 视频数量
    private Integer documentCount;   // 文档数量
    private Double completionRate;   // 完成率（learning_progress.completed=true / 选课人数）
}


