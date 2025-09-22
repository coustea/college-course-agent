package com.ccut.controller;

import com.ccut.entity.CourseVideo;
import com.ccut.entity.Result;
import com.ccut.mapper.CourseVideoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/course/video")
public class CourseVideoController {

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    // 新增视频元数据（URL 直链上传的场景）
    @PostMapping("/insert")
    public Result<String> insert(@RequestBody CourseVideo video){
        try {
            if (video.getUploadDate() == null) video.setUploadDate(new Date());
            int n = courseVideoMapper.insert(video);
            if (n>0) return Result.success("添加成功");
            return Result.error(500, "添加失败");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 修改视频元数据
    @PutMapping("/update")
    public Result<String> update(@RequestBody CourseVideo video){
        try {
            int n = courseVideoMapper.updateById(video);
            if (n>0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 删除视频
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("videoId") Long videoId){
        try {
            int n = courseVideoMapper.deleteById(videoId);
            if (n>0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 按课程列出视频
    @GetMapping("/list")
    public Result<List<CourseVideo>> list(@RequestParam("courseId") Long courseId){
        try {
            return Result.success(courseVideoMapper.findByCourseId(courseId));
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 可选：真正文件上传（若你有静态存储目录或对象存储，这里只示例回传 URL）
    @PostMapping("/upload")
    public Result<CourseVideo> upload(@RequestParam("courseId") Long courseId,
                                      @RequestParam("videoIndex") Integer videoIndex,
                                      @RequestParam(value = "videoTitle", required = false) String videoTitle,
                                      @RequestParam("file") MultipartFile file){
        try {
            // 这里应保存文件到本地或对象存储，返回可访问 URL。示例仅使用原文件名构造占位 URL。
            String url = "/media/videos/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            CourseVideo v = new CourseVideo();
            v.setCourseId(courseId);
            v.setVideoIndex(videoIndex);
            v.setVideoTitle(videoTitle);
            v.setVideoUrl(url);
            v.setDuration(0);
            v.setUploadDate(new Date());
            courseVideoMapper.insert(v);
            return Result.success(v);
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}


