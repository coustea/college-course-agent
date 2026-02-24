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
 * 文件存储服务实现（基于现有 UploadController 和 CourseDocumentServiceImpl）
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Value("${upload.base-dir:uploads}")
    private String baseDir;

    @Override
    public Attachment saveFile(MultipartFile file) {
        log.debug("执行方法：saveFile, 参数：file={}", file != null ? file.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (file == null || file.isEmpty()) {
                log.error("参数验证失败：文件不能为空");
                throw new IllegalArgumentException("文件不能为空");
            }

            // 按日期创建目录
            String dateDir = LocalDate.now().toString();
            Path uploadDir = Paths.get(baseDir, dateDir).toAbsolutePath();
            Files.createDirectories(uploadDir);
            log.debug("创建上传目录成功：path={}", uploadDir);

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
            log.info("文件保存成功：originalFilename={}, savedFilename={}, size={}", 
                    originalFilename, filename, file.getSize());

            // 生成访问 URL
            String url = "/uploads/" + dateDir + "/" + filename;

            // 创建附件对象
            Attachment attachment = new Attachment(
                getFileType(originalFilename),
                originalFilename,
                url,
                file.getSize(),
                file.getContentType()
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("文件保存完成：url={}, 耗时={}ms", url, duration);
            log.debug("方法返回：result={}", attachment);
            return attachment;

        } catch (Exception e) {
            log.error("文件保存失败：filename={}, error={}", file != null ? file.getOriginalFilename() : "null", e.getMessage(), e);
            throw new RuntimeException("文件保存失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<Attachment> saveFiles(List<MultipartFile> files) {
        log.debug("执行方法：saveFiles, 参数：files count={}", files != null ? files.size() : 0);
        long startTime = System.currentTimeMillis();
        try {
            List<Attachment> attachments = new ArrayList<>();
            if (files == null || files.isEmpty()) {
                log.warn("文件列表为空");
                return attachments;
            }

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Attachment attachment = saveFile(file);
                    attachments.add(attachment);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("批量保存文件成功：count={}, 耗时={}ms", attachments.size(), duration);
            log.debug("方法返回：result count={}", attachments.size());
            return attachments;
        } catch (Exception e) {
            log.error("批量保存文件失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getFileType(String filename) {
        log.debug("执行方法：getFileType, 参数：filename={}", filename);
        try {
            if (filename == null || filename.isEmpty()) {
                log.debug("文件名为空，返回其他类型");
                return "other";
            }

            String extension = StringUtils.getFilenameExtension(filename);
            if (extension == null) {
                log.debug("无扩展名，返回其他类型");
                return "other";
            }

            extension = extension.toLowerCase();

            // 图片类型
            if (extension.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
                log.debug("文件类型：image");
                return "image";
            }

            // 文档类型
            if (extension.matches("pdf|doc|docx|txt|xls|xlsx|ppt|pptx")) {
                log.debug("文件类型：document");
                return "document";
            }

            // 视频类型
            if (extension.matches("mp4|avi|mov|wmv|flv|mkv")) {
                log.debug("文件类型：video");
                return "video";
            }

            log.debug("文件类型：other");
            return "other";
        } catch (Exception e) {
            log.error("获取文件类型失败：filename={}, error={}", filename, e.getMessage(), e);
            return "other";
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        log.debug("执行方法：deleteFile, 参数：fileUrl={}", fileUrl);
        long startTime = System.currentTimeMillis();
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("文件 URL 为空");
                return false;
            }

            // 从 URL 中提取文件路径（去掉 /uploads/ 前缀）
            String relativePath = fileUrl.replace("/uploads/", "");
            Path fullPath = Paths.get(baseDir, relativePath).toAbsolutePath();

            File file = fullPath.toFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("文件删除成功：path={}, 耗时={}ms", fullPath, duration);
                } else {
                    log.warn("文件删除失败：path={}", fullPath);
                }
                log.debug("方法返回：result={}", deleted);
                return deleted;
            }

            log.warn("文件不存在：path={}", fullPath);
            log.debug("方法返回：result=false");
            return false;

        } catch (Exception e) {
            log.error("文件删除失败：fileUrl={}, error={}", fileUrl, e.getMessage(), e);
            return false;
        }
    }
}
