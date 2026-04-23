package com.ccut.service.Impl;

import com.ccut.entity.CourseVideo;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.service.CourseVideoService;
import org.mp4parser.IsoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CourseVideoServiceImpl.class);

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public CourseVideo insertVideo(Long courseId, Integer videoIndex, String videoTitle, Integer duration, MultipartFile file) {
        log.debug("执行方法：insertVideo, 参数：courseId={}, videoIndex={}, videoTitle={}, file={}", 
                courseId, videoIndex, videoTitle, file != null ? file.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (courseId == null) {
                log.error("参数验证失败：courseId 必填");
                throw new IllegalArgumentException("courseId 必填");
            }
            if (file == null || file.isEmpty()) {
                log.error("参数验证失败：视频文件必填");
                throw new IllegalArgumentException("视频文件必填");
            }

            // === 构造保存目录 ===
            String dateDir = LocalDate.now().toString();
            Path uploadPath = Paths.get(uploadDir, dateDir).toAbsolutePath();
            try {
                Files.createDirectories(uploadPath);
            } catch (Exception e) {
                log.error("创建目录失败：path={}", uploadPath);
                throw new RuntimeException("创建目录失败：" + e.getMessage());
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
                file.transferTo(target);
                log.info("视频文件保存成功：originalFilename={}, savedFilename={}", original, filename);
            } catch (Exception e) {
                log.error("文件保存失败：filename={}", filename);
                throw new RuntimeException("文件保存失败：" + e.getMessage());
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
                log.info("使用传入的视频时长：duration={}秒", duration);
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
                    log.info("自动读取视频时长成功：duration={}秒", seconds);
                } catch (Exception e) {
                    log.warn("自动读取视频时长失败，设置为 0: error={}", e.getMessage());
                    v.setDuration(0);
                }
            }

            v.setUploadDate(new Date());
            int n = courseVideoMapper.insert(v);
            long durationMs = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("视频插入成功：videoId={}, courseId={}, videoTitle={}, 耗时={}ms", 
                        v.getVideoId(), courseId, videoTitle, durationMs);
                log.debug("方法返回：result={}", v);
                return v;
            }
            log.error("视频插入失败：courseId={}, videoTitle={}", courseId, videoTitle);
            throw new RuntimeException("添加失败");
        } catch (Exception e) {
            log.error("视频插入异常：courseId={}, videoTitle={}, error={}", courseId, videoTitle, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String updateVideo(Long videoId, Integer videoIndex, String videoTitle, Integer duration, MultipartFile file) {
        log.debug("执行方法：updateVideo, 参数：videoId={}, videoIndex={}, videoTitle={}, file={}", 
                videoId, videoIndex, videoTitle, file != null ? file.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (videoId == null) {
                log.error("参数验证失败：videoId 必填");
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
                    log.error("创建目录失败：path={}", uploadPath);
                    throw new RuntimeException("创建目录失败：" + e.getMessage());
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
                    log.info("新视频文件保存成功：originalFilename={}, savedFilename={}", original, filename);
                } catch (Exception e) {
                    log.error("文件保存失败：filename={}", filename);
                    throw new RuntimeException("文件保存失败：" + e.getMessage());
                }

                String url = "/uploads/" + dateDir + "/" + filename;
                v.setVideoUrl(url);
                if (v.getDuration() == null) {
                    v.setDuration(0);
                }
                log.info("视频文件已更新：url={}", url);
            }

            int n = courseVideoMapper.updateById(v);
            long durationMs = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("视频更新成功：videoId={}, 耗时={}ms", videoId, durationMs);
                log.debug("方法返回：result=更新成功");
                return "更新成功";
            }
            log.warn("视频未找到或未变更：videoId={}, 耗时={}ms", videoId, durationMs);
            throw new RuntimeException("未找到或未变更");
        } catch (Exception e) {
            log.error("视频更新异常：videoId={}, error={}", videoId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String deleteVideo(Long videoId) {
        log.debug("执行方法：deleteVideo, 参数：videoId={}", videoId);
        long startTime = System.currentTimeMillis();
        try {
            int n = courseVideoMapper.deleteById(videoId);
            long durationMs = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("视频删除成功：videoId={}, 耗时={}ms", videoId, durationMs);
                log.debug("方法返回：result=删除成功");
                return "删除成功";
            }
            log.warn("视频未找到：videoId={}, 耗时={}ms", videoId, durationMs);
            throw new RuntimeException("未找到");
        } catch (Exception e) {
            log.error("视频删除失败：videoId={}, error={}", videoId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<CourseVideo> listByCourseId(Long courseId) {
        log.debug("执行方法：listByCourseId, 参数：courseId={}", courseId);
        long startTime = System.currentTimeMillis();
        try {
            List<CourseVideo> result = courseVideoMapper.findByCourseId(courseId);
            long durationMs = System.currentTimeMillis() - startTime;
            log.info("查询课程视频列表成功：courseId={}, count={}, 耗时={}ms", courseId, result != null ? result.size() : 0, durationMs);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询课程视频列表失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

}
