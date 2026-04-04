package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.CourseVideo;
import com.ccut.service.CourseVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程视频控制器
 */
@RestController
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
        CourseVideo video = courseVideoService.insertVideo(courseId, videoIndex, videoTitle, duration, file);
        return Result.success(video);
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
        String result = courseVideoService.updateVideo(videoId, videoIndex, videoTitle, duration, file);
        return Result.success(result);
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("videoId") Long videoId) {
        String result = courseVideoService.deleteVideo(videoId);
        return Result.success(result);
    }

    /**
     * 按课程列出视频
     */
    @GetMapping("/list")
    public Result<List<CourseVideo>> list(@RequestParam("courseId") Long courseId) {
        List<CourseVideo> videos = courseVideoService.listByCourseId(courseId);
        return Result.success(videos);
    }
}
