package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherVideoItem {
    // 兼容前端常用命名（冗余别名）
    private Long id;              // = videoId
    private Long courseId;
    private String courseName;    // = courseTitle
    private String courseTitle;   // 别名，便于直接展示
    private Long videoId;
    private Integer videoIndex;
    private String videoTitle;    // = title
    private String title;         // 别名
    private String videoUrl;      // = url
    private String url;           // 别名
    private Integer duration;
    private Date uploadDate;

    // 可选统计字段
    private Integer studentCount;
}


