package com.ccut.service;

import com.ccut.entity.CourseVideo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程视频服务接口
 */
public interface CourseVideoService {

    /**
     * 上传视频
     * @param courseId 课程ID
     * @param videoIndex 视频序号（可选）
     * @param videoTitle 视频标题（可选）
     * @param duration 视频时长（可选，秒）
     * @param file 视频文件
     * @return 上传后的课程视频
     */
    CourseVideo insertVideo(Long courseId, Integer videoIndex, String videoTitle, Integer duration, MultipartFile file);

    /**
     * 更新视频
     * @param videoId 视频ID
     * @param videoIndex 视频序号（可选）
     * @param videoTitle 视频标题（可选）
     * @param duration 视频时长（可选，秒）
     * @param file 视频文件（可选）
     * @return 更新结果消息
     */
    String updateVideo(Long videoId, Integer videoIndex, String videoTitle, Integer duration, MultipartFile file);

    /**
     * 删除视频
     * @param videoId 视频ID
     * @return 删除结果消息
     */
    String deleteVideo(Long videoId);

    /**
     * 按课程列出视频
     * @param courseId 课程ID
     * @return 视频列表
     */
    List<CourseVideo> listByCourseId(Long courseId);

}
