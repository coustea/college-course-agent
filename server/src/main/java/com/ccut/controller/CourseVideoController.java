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
        log.debug("收到上传视频请求：URI=/api/course/video/insert, 参数：courseId={}, videoIndex={}, videoTitle={}, duration={}, 文件名={}, 文件大小={} bytes",
                courseId, videoIndex, videoTitle, duration, file.getOriginalFilename(), file.getSize());
        try {
            log.info("执行上传视频业务：courseId={}, fileName={}, fileSize={} bytes", courseId, file.getOriginalFilename(), file.getSize());
            long startTime = System.currentTimeMillis();
            CourseVideo video = courseVideoService.insertVideo(courseId, videoIndex, videoTitle, duration, file);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("上传视频成功：courseId={}, videoId={}, fileName={}, 耗时={} ms", courseId, video.getVideoId(), file.getOriginalFilename(), costTime);
            return Result.success(video);
        } catch (IllegalArgumentException e) {
            log.warn("上传视频参数错误：courseId={}, 错误：{}", courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("上传视频业务异常：courseId={}, 错误：{}", courseId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("上传视频异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
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
        log.debug("收到更新视频请求：URI=/api/course/video/update, 参数：videoId={}, videoIndex={}, videoTitle={}, 有文件={}",
                videoId, videoIndex, videoTitle, file != null && !file.isEmpty());
        try {
            log.info("执行更新视频业务：videoId={}", videoId);
            long startTime = System.currentTimeMillis();
            String result = courseVideoService.updateVideo(videoId, videoIndex, videoTitle, duration, file);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("更新视频成功：videoId={}, 耗时={} ms", videoId, costTime);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("更新视频参数错误：videoId={}, 错误：{}", videoId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新视频业务异常：videoId={}, 错误：{}", videoId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新视频异常：videoId={}, 错误：{}", videoId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("videoId") Long videoId) {
        log.debug("收到删除视频请求：URI=/api/course/video/delete, 参数：videoId={}", videoId);
        try {
            log.info("执行删除视频业务：videoId={}", videoId);
            String result = courseVideoService.deleteVideo(videoId);
            log.debug("删除视频成功：videoId={}", videoId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("删除视频业务异常：videoId={}, 错误：{}", videoId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("删除视频异常：videoId={}, 错误：{}", videoId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按课程列出视频
     */
    @GetMapping("/list")
    public Result<List<CourseVideo>> list(@RequestParam("courseId") Long courseId) {
        log.debug("收到查询视频列表请求：URI=/api/course/video/list, 参数：courseId={}", courseId);
        try {
            log.info("执行查询视频列表业务：courseId={}", courseId);
            List<CourseVideo> videos = courseVideoService.listByCourseId(courseId);
            log.debug("查询视频列表成功：courseId={}, 视频数={}", courseId, videos.size());
            return Result.success(videos);
        } catch (Exception e) {
            log.error("查询视频列表异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
