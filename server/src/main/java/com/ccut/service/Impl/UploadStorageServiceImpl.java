package com.ccut.service.Impl;

import com.ccut.dto.StoredUpload;
import com.ccut.service.UploadStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * 本地上传存储实现，隐藏日期目录、随机文件名和 /uploads URL 拼接细节。
 */
@Slf4j
@Service
public class UploadStorageServiceImpl implements UploadStorageService {

    private static final String PUBLIC_URL_PREFIX = "/uploads/";

    @Value("${file.upload-dir:${upload.base-dir:uploads}}")
    private String uploadDir;

    @Override
    public StoredUpload store(MultipartFile file, String urlDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            String dateDir = LocalDate.now().toString();
            String safeDirectory = normalizeUrlDirectory(urlDirectory);
            Path root = uploadRoot();
            Path datedDir = safeDirectory.isBlank()
                    ? root.resolve(dateDir)
                    : root.resolve(safeDirectory).resolve(dateDir);
            Files.createDirectories(datedDir);

            String originalFilename = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String savedFilename = UUID.randomUUID().toString().replace("-", "");
            if (StringUtils.hasText(extension)) {
                savedFilename += "." + extension.toLowerCase();
            }

            Path target = datedDir.resolve(savedFilename).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("非法文件路径");
            }

            file.transferTo(target.toFile());

            String url = PUBLIC_URL_PREFIX
                    + (safeDirectory.isBlank() ? "" : safeDirectory + "/")
                    + dateDir + "/" + savedFilename;
            log.info("上传文件保存成功：originalFilename={}, savedFilename={}, url={}",
                    originalFilename, savedFilename, url);
            return new StoredUpload(
                    originalFilename,
                    savedFilename,
                    url,
                    target,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件保存失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || !fileUrl.startsWith(PUBLIC_URL_PREFIX)) {
            return false;
        }

        try {
            Path root = uploadRoot();
            String relativePath = fileUrl.substring(PUBLIC_URL_PREFIX.length());
            Path target = root.resolve(relativePath).normalize();
            if (!target.startsWith(root) || !Files.isRegularFile(target)) {
                return false;
            }
            return Files.deleteIfExists(target);
        } catch (Exception e) {
            log.error("删除上传文件失败：fileUrl={}, error={}", fileUrl, e.getMessage(), e);
            return false;
        }
    }

    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String normalizeUrlDirectory(String urlDirectory) {
        if (urlDirectory == null || urlDirectory.isBlank()) {
            return "";
        }
        String normalized = urlDirectory.replace('\\', '/');
        normalized = Arrays.stream(normalized.split("/"))
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("/"));
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("非法上传目录");
        }
        return normalized;
    }
}
