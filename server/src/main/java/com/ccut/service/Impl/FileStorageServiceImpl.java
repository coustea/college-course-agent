package com.ccut.service.Impl;

import com.ccut.dto.Attachment;
import com.ccut.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务实现（基于现有UploadController和CourseDocumentServiceImpl）
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Value("${upload.base-dir:uploads}")
    private String baseDir;

    @Override
    public Attachment saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            // 按日期创建目录
            String dateDir = LocalDate.now().toString();
            Path uploadDir = Paths.get(baseDir, dateDir).toAbsolutePath();
            Files.createDirectories(uploadDir);

            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            String ext = StringUtils.getFilenameExtension(originalFilename);

            // 生成唯一文件名
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (StringUtils.hasText(ext)) {
                filename = filename + "." + ext.toLowerCase();
            }

            // 保存文件
            Path target = uploadDir.resolve(filename);
            file.transferTo(target.toFile());
            logger.info("Saved file: {}", target);

            // 生成访问URL
            String url = "/uploads/" + dateDir + "/" + filename;

            // 创建附件对象
            Attachment attachment = new Attachment(
                getFileType(originalFilename),
                originalFilename,
                url,
                file.getSize(),
                file.getContentType()
            );

            return attachment;

        } catch (Exception e) {
            logger.error("Failed to save file: {}", e.getMessage(), e);
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Attachment> saveFiles(List<MultipartFile> files) {
        List<Attachment> attachments = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return attachments;
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Attachment attachment = saveFile(file);
                attachments.add(attachment);
            }
        }

        return attachments;
    }

    @Override
    public String getFileType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "other";
        }

        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null) {
            return "other";
        }

        extension = extension.toLowerCase();

        // 图片类型
        if (extension.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
            return "image";
        }

        // 文档类型
        if (extension.matches("pdf|doc|docx|txt|xls|xlsx|ppt|pptx")) {
            return "document";
        }

        // 视频类型
        if (extension.matches("mp4|avi|mov|wmv|flv|mkv")) {
            return "video";
        }

        return "other";
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            // 从URL中提取文件路径（去掉 /uploads/ 前缀）
            String relativePath = fileUrl.replace("/uploads/", "");
            Path fullPath = Paths.get(baseDir, relativePath).toAbsolutePath();

            File file = fullPath.toFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    logger.info("Deleted file: {}", fullPath);
                }
                return deleted;
            }

            return false;

        } catch (Exception e) {
            logger.error("Failed to delete file: {}", e.getMessage(), e);
            return false;
        }
    }
}