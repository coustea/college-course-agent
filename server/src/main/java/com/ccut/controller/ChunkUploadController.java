package com.ccut.controller;

import com.ccut.entity.CourseVideo;
import com.ccut.dto.Result;
import com.ccut.mapper.CourseVideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片上传控制器
 * 支持大文件分片上传和断点续传
 */
@Slf4j
@RestController
@RequestMapping("/api/chunk")
public class ChunkUploadController {

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 临时存储分片信息的 Map
    private static final Map<String, ChunkInfo> chunkInfoMap = new ConcurrentHashMap<>();

    /**
     * 初始化分片上传
     */
    @PostMapping("/init")
    public Result<Map<String, Object>> initUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("totalChunks") Integer totalChunks
    ) {
        log.debug("收到初始化分片上传请求：URI=/api/chunk/init, 参数：fileName={}, fileSize={} bytes, totalChunks={}",
                fileName, fileSize, totalChunks);
        try {
            log.info("执行初始化分片上传业务：fileName={}, fileSize={} bytes", fileName, fileSize);
            String uploadId = UUID.randomUUID().toString().replace("-", "");
            ChunkInfo info = new ChunkInfo();
            info.setUploadId(uploadId);
            info.setFileName(fileName);
            info.setFileSize(fileSize);
            info.setTotalChunks(totalChunks);
            info.setUploadedChunks(new HashSet<>());
            info.setCreateTime(System.currentTimeMillis());

            chunkInfoMap.put(uploadId, info);

            Map<String, Object> result = new HashMap<>();
            result.put("uploadId", uploadId);
            result.put("chunkSize", 2 * 1024 * 1024); // 2MB per chunk

            log.debug("初始化分片上传成功：fileName={}, uploadId={}", fileName, uploadId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("初始化分片上传失败：fileName={}, 错误：{}", fileName, e.getMessage(), e);
            return Result.error(500, "初始化上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestPart("chunk") MultipartFile chunk
    ) {
        log.debug("收到上传分片请求：URI=/api/chunk/upload, 参数：uploadId={}, chunkIndex={}, chunkSize={} bytes",
                uploadId, chunkIndex, chunk.getSize());
        try {
            ChunkInfo info = chunkInfoMap.get(uploadId);
            if (info == null) {
                log.warn("上传分片失败：无效的 uploadId={}", uploadId);
                return Result.error(400, "无效的 uploadId 或上传已过期");
            }

            // 创建临时目录
            String tempDir = uploadDir + "/temp/" + uploadId;
            Path tempPath = Paths.get(tempDir);
            Files.createDirectories(tempPath);

            // 保存分片
            String chunkFileName = String.format("chunk_%d", chunkIndex);
            Path chunkPath = tempPath.resolve(chunkFileName);
            chunk.transferTo(chunkPath.toFile());

            // 记录已上传的分片
            info.getUploadedChunks().add(chunkIndex);

            Map<String, Object> result = new HashMap<>();
            result.put("uploadedChunks", info.getUploadedChunks().size());
            result.put("totalChunks", info.getTotalChunks());
            result.put("isComplete", info.getUploadedChunks().size() == info.getTotalChunks());

            log.debug("上传分片成功：uploadId={}, chunkIndex={}, 进度={}/{}",
                    uploadId, chunkIndex, info.getUploadedChunks().size(), info.getTotalChunks());
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传分片失败：uploadId={}, chunkIndex={}, 错误：{}", uploadId, chunkIndex, e.getMessage(), e);
            return Result.error(500, "分片上传失败：" + e.getMessage());
        }
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public Result<CourseVideo> mergeChunks(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "videoIndex", required = false) Integer videoIndex,
            @RequestParam(value = "videoTitle", required = false) String videoTitle,
            @RequestParam(value = "duration", required = false) Integer duration
    ) {
        log.debug("收到合并分片请求：URI=/api/chunk/merge, 参数：uploadId={}, courseId={}, videoTitle={}",
                uploadId, courseId, videoTitle);
        try {
            ChunkInfo info = chunkInfoMap.get(uploadId);
            if (info == null) {
                log.warn("合并分片失败：无效的 uploadId={}", uploadId);
                return Result.error(400, "无效的 uploadId");
            }

            // 检查是否所有分片都已上传
            if (info.getUploadedChunks().size() != info.getTotalChunks()) {
                log.warn("合并分片失败：还有分片未上传完成，uploadId={}, uploaded={}/{}",
                        uploadId, info.getUploadedChunks().size(), info.getTotalChunks());
                return Result.error(400, "还有分片未上传完成");
            }

            log.info("执行合并分片业务：uploadId={}, courseId={}, fileName={}, fileSize={} bytes",
                    uploadId, courseId, info.getFileName(), info.getFileSize());
            long startTime = System.currentTimeMillis();

            // 创建最终保存目录
            String dateDir = LocalDate.now().toString();
            Path uploadPath = Paths.get(uploadDir, dateDir);
            Files.createDirectories(uploadPath);

            // 构造最终文件名
            String originalName = info.getFileName();
            String ext = (originalName != null && originalName.contains(".")) ?
                    originalName.substring(originalName.lastIndexOf('.')) : "";
            String finalFileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path finalPath = uploadPath.resolve(finalFileName);

            // 合并分片
            String tempDir = uploadDir + "/temp/" + uploadId;
            try (FileOutputStream fos = new FileOutputStream(finalPath.toFile())) {
                for (int i = 0; i < info.getTotalChunks(); i++) {
                    Path chunkPath = Paths.get(tempDir, String.format("chunk_%d", i));
                    Files.copy(chunkPath, fos);
                }
            }

            // 删除临时分片文件
            deleteDirectory(new File(tempDir));

            // 从 map 中移除
            chunkInfoMap.remove(uploadId);

            // 数据库存储的访问路径
            String url = "/uploads/" + dateDir + "/" + finalFileName;

            // 构建 CourseVideo 实体
            CourseVideo v = new CourseVideo();
            v.setCourseId(courseId);
            v.setVideoTitle(videoTitle != null ? videoTitle : originalName);
            v.setVideoUrl(url);

            // 自动计算序号
            if (videoIndex == null) {
                Integer max = courseVideoMapper.findMaxIndexByCourseId(courseId);
                v.setVideoIndex((max == null || max <= 0) ? 1 : max + 1);
            } else {
                v.setVideoIndex(videoIndex);
            }

            // 设置时长
            v.setDuration(duration != null ? duration : 0);
            v.setUploadDate(new Date());

            int n = courseVideoMapper.insert(v);
            long costTime = System.currentTimeMillis() - startTime;
            
            if (n > 0) {
                log.info("合并分片成功：uploadId={}, courseId={}, videoId={}, fileName={}, 耗时={} ms",
                        uploadId, courseId, v.getVideoId(), finalFileName, costTime);
                return Result.success(v);
            }
            log.error("合并分片失败：保存数据库失败，uploadId={}, courseId={}", uploadId, courseId);
            return Result.error(500, "保存数据库失败");
        } catch (Exception e) {
            log.error("合并分片失败：uploadId={}, courseId={}, 错误：{}", uploadId, courseId, e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "合并分片失败：" + e.getMessage());
        }
    }

    /**
     * 检查上传状态（用于断点续传）
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> checkStatus(@RequestParam("uploadId") String uploadId) {
        log.debug("收到检查上传状态请求：URI=/api/chunk/status, 参数：uploadId={}", uploadId);
        try {
            ChunkInfo info = chunkInfoMap.get(uploadId);
            if (info == null) {
                log.warn("检查上传状态失败：未找到上传任务，uploadId={}", uploadId);
                return Result.error(404, "未找到上传任务");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("uploadedChunks", new ArrayList<>(info.getUploadedChunks()));
            result.put("totalChunks", info.getTotalChunks());
            result.put("uploadedSize", info.getUploadedChunks().size());

            log.debug("检查上传状态成功：uploadId={}, 进度={}/{}", uploadId, info.getUploadedChunks().size(), info.getTotalChunks());
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查上传状态失败：uploadId={}, 错误：{}", uploadId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 取消上传
     */
    @DeleteMapping("/cancel")
    public Result<String> cancelUpload(@RequestParam("uploadId") String uploadId) {
        log.debug("收到取消上传请求：URI=/api/chunk/cancel, 参数：uploadId={}", uploadId);
        try {
            ChunkInfo info = chunkInfoMap.remove(uploadId);
            if (info != null) {
                log.info("执行取消上传业务：uploadId={}, fileName={}", uploadId, info.getFileName());
                // 删除临时文件
                String tempDir = uploadDir + "/temp/" + uploadId;
                deleteDirectory(new File(tempDir));
                log.debug("取消上传成功：uploadId={}", uploadId);
            } else {
                log.warn("取消上传失败：未找到上传任务，uploadId={}", uploadId);
            }
            return Result.success("取消成功");
        } catch (Exception e) {
            log.error("取消上传失败：uploadId={}, 错误：{}", uploadId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * 分片信息类
     */
    static class ChunkInfo {
        private String uploadId;
        private String fileName;
        private Long fileSize;
        private Integer totalChunks;
        private Set<Integer> uploadedChunks;
        private Long createTime;

        // Getters and Setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public Integer getTotalChunks() { return totalChunks; }
        public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
        public Set<Integer> getUploadedChunks() { return uploadedChunks; }
        public void setUploadedChunks(Set<Integer> uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public Long getCreateTime() { return createTime; }
        public void setCreateTime(Long createTime) { this.createTime = createTime; }
    }
}
