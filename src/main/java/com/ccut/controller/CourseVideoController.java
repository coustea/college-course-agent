package com.ccut.controller;

import com.ccut.entity.CourseVideo;
import com.ccut.entity.Result;
import com.ccut.mapper.CourseVideoMapper;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import java.io.FileInputStream;
import org.mp4parser.IsoFile;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/course/video")
public class CourseVideoController {

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    // 改造为：上传视频文件，courseId 必填、视频文件必填，其它（videoIndex、videoTitle、duration）选填
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CourseVideo> insert(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestPart("file") MultipartFile file
    ){
        try {
            if (courseId == null) return Result.error(400, "courseId 必填");
            if (file == null || file.isEmpty()) return Result.error(400, "视频文件必填");

            // 保存到 绝对路径 uploads/yyyy-MM-dd/uuid.ext，并确保父目录存在
            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get("uploads").toAbsolutePath();
            Files.createDirectories(baseDir);
            Path uploadDir = baseDir.resolve(dateDir);
            Files.createDirectories(uploadDir);

            String original = file.getOriginalFilename();
            String ext = null;
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            }
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (ext != null && !ext.isEmpty()) {
                filename = filename + "." + ext;
            }
            Path target = uploadDir.resolve(filename);
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());

            String url = "/uploads/" + dateDir + "/" + filename;

            CourseVideo v = new CourseVideo();
            v.setCourseId(courseId);
            // 自动分配 videoIndex：未传则取该课程最大值+1；若无记录则置 1
            if (videoIndex == null) {
                Integer max = courseVideoMapper.findMaxIndexByCourseId(courseId);
                int next = (max == null || max <= 0) ? 1 : max + 1;
                v.setVideoIndex(next);
            } else {
                v.setVideoIndex(videoIndex);
            }
            v.setVideoTitle(videoTitle);
            v.setVideoUrl(url);
            if (duration != null) {
                v.setDuration(duration);
            } else {
                // 自动读取视频时长（秒），失败则置 0
                try (FileInputStream fis = new FileInputStream(target.toFile())) {
                    IsoFile isoFile = new IsoFile(fis.getChannel());
                    long durationCurrent = isoFile.getMovieBox().getMovieHeaderBox().getDuration();
                    long timescale = isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
                    int seconds = 0;
                    if (timescale > 0) {
                        seconds = new BigDecimal(durationCurrent)
                                .divide(new BigDecimal(timescale), 0, java.math.RoundingMode.HALF_UP)
                                .intValue();
                    }
                    v.setDuration(Math.max(0, seconds));
                    isoFile.close();
                } catch (Exception ignore) {
                    v.setDuration(0);
                }
            }
            v.setUploadDate(new Date());
            int n = courseVideoMapper.insert(v);
            if (n > 0) return Result.success(v);
            return Result.error(500, "添加失败");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 修改：改为 multipart 上传，支持替换视频文件与更新元数据
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam("videoId") Long videoId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestPart(value = "file", required = false) MultipartFile file
    ){
        try {
            if (videoId == null) return Result.error(400, "videoId 必填");

            CourseVideo v = new CourseVideo();
            v.setVideoId(videoId);
            v.setVideoIndex(videoIndex);
            v.setVideoTitle(videoTitle);
            if (duration != null) v.setDuration(duration);

            // 若上传了新文件，则保存并更新 URL
            if (file != null && !file.isEmpty()) {
                String dateDir = LocalDate.now().toString();
                Path baseDir = Paths.get("uploads").toAbsolutePath();
                Files.createDirectories(baseDir);
                Path uploadDir = baseDir.resolve(dateDir);
                Files.createDirectories(uploadDir);

                String original = file.getOriginalFilename();
                String ext = null;
                if (original != null && original.contains(".")) {
                    ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                }
                String filename = UUID.randomUUID().toString().replace("-", "");
                if (ext != null && !ext.isEmpty()) {
                    filename = filename + "." + ext;
                }
                Path target = uploadDir.resolve(filename);
                Files.createDirectories(target.getParent());
                file.transferTo(target.toFile());

                String url = "/uploads/" + dateDir + "/" + filename;
                v.setVideoUrl(url);
                if (v.getDuration() == null) v.setDuration(0);
            }

            int n = courseVideoMapper.updateById(v);
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


