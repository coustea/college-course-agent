package com.ccut.service.Impl;

import com.ccut.entity.CourseVideo;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.service.CourseVideoService;
import org.mp4parser.IsoFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 课程视频服务实现类
 */
@Service
public class CourseVideoServiceImpl implements CourseVideoService {

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public CourseVideo insertVideo(Long courseId, Integer videoIndex, String videoTitle, Integer duration, MultipartFile file) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId 必填");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("视频文件必填");
        }

        // === 构造保存目录 ===
        String dateDir = LocalDate.now().toString();
        Path uploadPath = Paths.get(uploadDir, dateDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (Exception e) {
            throw new RuntimeException("创建目录失败: " + e.getMessage());
        }

        // === 构造文件名 ===
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ?
                original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (!ext.isEmpty()) {
            filename += "." + ext;
        }

        Path target = uploadPath.resolve(filename);
        try {
            file.transferTo(target.toFile());
        } catch (Exception e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

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
                        ? new BigDecimal(d).divide(new BigDecimal(scale), 0, RoundingMode.HALF_UP).intValue()
                        : 0;
                v.setDuration(Math.max(0, seconds));
                isoFile.close();
            } catch (Exception e) {
                v.setDuration(0);
            }
        }

        v.setUploadDate(new Date());
        int n = courseVideoMapper.insert(v);
        if (n > 0) {
            return v;
        }
        throw new RuntimeException("添加失败");
    }

    @Override
    @Transactional
    public String updateVideo(Long videoId, Integer videoIndex, String videoTitle, Integer duration, MultipartFile file) {
        if (videoId == null) {
            throw new IllegalArgumentException("videoId 必填");
        }

        CourseVideo v = new CourseVideo();
        v.setVideoId(videoId);
        v.setVideoIndex(videoIndex);
        v.setVideoTitle(videoTitle);
        if (duration != null) {
            v.setDuration(duration);
        }

        // === 若上传了新文件 ===
        if (file != null && !file.isEmpty()) {
            String dateDir = LocalDate.now().toString();
            Path uploadPath = Paths.get(uploadDir, dateDir);
            try {
                Files.createDirectories(uploadPath);
            } catch (Exception e) {
                throw new RuntimeException("创建目录失败: " + e.getMessage());
            }

            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains(".")) ?
                    original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (!ext.isEmpty()) {
                filename += "." + ext;
            }

            Path target = uploadPath.resolve(filename);
            try {
                file.transferTo(target.toFile());
            } catch (Exception e) {
                throw new RuntimeException("文件保存失败: " + e.getMessage());
            }

            String url = "/uploads/" + dateDir + "/" + filename;
            v.setVideoUrl(url);
            if (v.getDuration() == null) {
                v.setDuration(0);
            }
        }

        int n = courseVideoMapper.updateById(v);
        if (n > 0) {
            return "更新成功";
        }
        throw new RuntimeException("未找到或未变更");
    }

    @Override
    @Transactional
    public String deleteVideo(Long videoId) {
        int n = courseVideoMapper.deleteById(videoId);
        if (n > 0) {
            return "删除成功";
        }
        throw new RuntimeException("未找到");
    }

    @Override
    public List<CourseVideo> listByCourseId(Long courseId) {
        return courseVideoMapper.findByCourseId(courseId);
    }

}
