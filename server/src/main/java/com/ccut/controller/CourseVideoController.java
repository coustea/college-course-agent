package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.CourseVideo;
import com.ccut.service.CourseVideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程视频控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/course/video")
public class CourseVideoController {

    @Autowired
    private CourseVideoService courseVideoService;

    /**
     * 上传视频
     */
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CourseVideo> insert(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            return Result.success(courseVideoService.insertVideo(courseId, videoIndex, videoTitle, duration, file));
        } catch (IllegalArgumentException e) {
            log.warn("上传视频参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("上传视频业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("上传视频异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新视频
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam("videoId") Long videoId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            return Result.success(courseVideoService.updateVideo(videoId, videoIndex, videoTitle, duration, file));
        } catch (IllegalArgumentException e) {
            log.warn("更新视频参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新视频业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新视频异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("videoId") Long videoId) {
        try {
            return Result.success(courseVideoService.deleteVideo(videoId));
        } catch (RuntimeException e) {
            log.warn("删除视频业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("删除视频异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按课程列出视频
     */
    @GetMapping("/list")
    public Result<List<CourseVideo>> list(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(courseVideoService.listByCourseId(courseId));
        } catch (Exception e) {
            log.error("查询视频列表异常", e);
            return Result.error(500, e.getMessage());
        }
    }

}
