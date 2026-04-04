package com.ccut.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api")
public class UploadController {

    @Value("${upload.base-dir:uploads}")
    private String baseDir;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        String dateDir = LocalDate.now().toString();
        Path uploadDir = Paths.get(baseDir, dateDir).toAbsolutePath();
        Files.createDirectories(uploadDir);

        String original = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(ext)) filename = filename + "." + ext.toLowerCase();

        Path target = uploadDir.resolve(filename);
        file.transferTo(target.toFile());

        String url = "/uploads/" + dateDir + "/" + filename;

        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("fileName", original);
        data.put("fileSize", file.getSize());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return resp;
    }
}
