package com.ccut.controller;

import com.ccut.entity.CourseVideo;
import com.ccut.entity.Result;
import com.ccut.mapper.CourseVideoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.mp4parser.IsoFile;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/course/video")
public class CourseVideoController {

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    // 从 application.yml 读取上传路径
    @Value("${file.upload-dir}")
    private String uploadDir;

    // ======================== 上传视频 ========================
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CourseVideo> insert(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            if (courseId == null) return Result.error(400, "courseId 必填");
            if (file == null || file.isEmpty()) return Result.error(400, "视频文件必填");

            // === 构造保存目录 ===
            String dateDir = LocalDate.now().toString();
            Path uploadPath = Paths.get(uploadDir, dateDir);
            Files.createDirectories(uploadPath);

            // === 构造文件名 ===
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains(".")) ?
                    original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (!ext.isEmpty()) filename += "." + ext;

            Path target = uploadPath.resolve(filename);
            file.transferTo(target.toFile());

            // === 数据库存储的访问路径 ===
            String url = "/uploads/" + dateDir + "/" + filename;

            // === 构建 CourseVideo 实体 ===
            CourseVideo v = new CourseVideo();
            v.setCourseId(courseId);
            v.setVideoTitle(videoTitle);
            v.setVideoUrl(url);

            // 自动计算序号
            if (videoIndex == null) {
                Integer max = courseVideoMapper.findMaxIndexByCourseId(courseId);
                v.setVideoIndex((max == null || max <= 0) ? 1 : max + 1);
            } else {
                v.setVideoIndex(videoIndex);
            }

            // 自动读取视频时长（秒）
            if (duration != null) {
                v.setDuration(duration);
            } else {
                try (FileInputStream fis = new FileInputStream(target.toFile())) {
                    IsoFile isoFile = new IsoFile(fis.getChannel());
                    long d = isoFile.getMovieBox().getMovieHeaderBox().getDuration();
                    long scale = isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
                    int seconds = (scale > 0)
                            ? new BigDecimal(d).divide(new BigDecimal(scale), 0, BigDecimal.ROUND_HALF_UP).intValue()
                            : 0;
                    v.setDuration(Math.max(0, seconds));
                    isoFile.close();
                } catch (Exception e) {
                    v.setDuration(0);
                }
            }

            v.setUploadDate(new Date());
            int n = courseVideoMapper.insert(v);
            if (n > 0) return Result.success(v);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ======================== 更新视频 ========================
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam("videoId") Long videoId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            if (videoId == null) return Result.error(400, "videoId 必填");

            CourseVideo v = new CourseVideo();
            v.setVideoId(videoId);
            v.setVideoIndex(videoIndex);
            v.setVideoTitle(videoTitle);
            if (duration != null) v.setDuration(duration);

            // === 若上传了新文件 ===
            if (file != null && !file.isEmpty()) {
                String dateDir = LocalDate.now().toString();
                Path uploadPath = Paths.get(uploadDir, dateDir);
                Files.createDirectories(uploadPath);

                String original = file.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ?
                        original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
                String filename = UUID.randomUUID().toString().replace("-", "");
                if (!ext.isEmpty()) filename += "." + ext;

                Path target = uploadPath.resolve(filename);
                file.transferTo(target.toFile());

                String url = "/uploads/" + dateDir + "/" + filename;
                v.setVideoUrl(url);
                if (v.getDuration() == null) v.setDuration(0);
            }

            int n = courseVideoMapper.updateById(v);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ======================== 删除视频 ========================
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("videoId") Long videoId) {
        try {
            int n = courseVideoMapper.deleteById(videoId);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ======================== 按课程列出视频 ========================
    @GetMapping("/list")
    public Result<List<CourseVideo>> list(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(courseVideoMapper.findByCourseId(courseId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
