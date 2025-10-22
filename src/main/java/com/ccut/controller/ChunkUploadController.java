package com.ccut.controller;

import com.ccut.entity.CourseVideo;
import com.ccut.entity.Result;
import com.ccut.mapper.CourseVideoMapper;
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
@RestController
@RequestMapping("/api/chunk")
public class ChunkUploadController {

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 临时存储分片信息的Map
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
        try {
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
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "初始化上传失败: " + e.getMessage());
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
        try {
            ChunkInfo info = chunkInfoMap.get(uploadId);
            if (info == null) {
                return Result.error(400, "无效的uploadId或上传已过期");
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
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "分片上传失败: " + e.getMessage());
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
        try {
            ChunkInfo info = chunkInfoMap.get(uploadId);
            if (info == null) {
                return Result.error(400, "无效的uploadId");
            }

            // 检查是否所有分片都已上传
            if (info.getUploadedChunks().size() != info.getTotalChunks()) {
                return Result.error(400, "还有分片未上传完成");
            }

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

            // 从map中移除
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
            if (n > 0) {
                return Result.success(v);
            }
            return Result.error(500, "保存数据库失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 检查上传状态（用于断点续传）
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> checkStatus(@RequestParam("uploadId") String uploadId) {
        try {
            ChunkInfo info = chunkInfoMap.get(uploadId);
            if (info == null) {
                return Result.error(404, "未找到上传任务");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("uploadedChunks", new ArrayList<>(info.getUploadedChunks()));
            result.put("totalChunks", info.getTotalChunks());
            result.put("uploadedSize", info.getUploadedChunks().size());
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 取消上传
     */
    @DeleteMapping("/cancel")
    public Result<String> cancelUpload(@RequestParam("uploadId") String uploadId) {
        try {
            ChunkInfo info = chunkInfoMap.remove(uploadId);
            if (info != null) {
                // 删除临时文件
                String tempDir = uploadDir + "/temp/" + uploadId;
                deleteDirectory(new File(tempDir));
            }
            return Result.success("取消成功");
        } catch (Exception e) {
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

