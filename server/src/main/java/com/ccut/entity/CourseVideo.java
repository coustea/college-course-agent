package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseVideo {
    private Long videoId;         // 视频ID
    private Long courseId;        // 课程ID
    private Integer videoIndex;   // 视频集数（顺序编号）
    private String videoTitle;    // 视频标题
    private String videoUrl;      // 视频URL地址
    private Integer duration;     // 视频时长（秒）
    private Date uploadDate;      // 上传日期
}


