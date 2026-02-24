package com.ccut.controller;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api")
public class UploadController {

    @Value("${upload.base-dir:uploads}")
    private String baseDir;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) throws IOException {
        log.debug("收到文件上传请求：URI=/api/upload, 参数：fileName={}, fileSize={} bytes",
                file.getOriginalFilename(), file.getSize());
        Map<String, Object> resp = new HashMap<>();
        
        if (file == null || file.isEmpty()) {
            log.warn("文件上传失败：文件为空");
            resp.put("success", false);
            resp.put("message", "文件为空");
            return resp;
        }

        try {
            long startTime = System.currentTimeMillis();
            
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
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("文件上传成功：fileName={}, fileSize={} bytes, url={}, 耗时={} ms",
                    original, file.getSize(), url, costTime);
            
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            data.put("fileName", original);
            data.put("fileSize", file.getSize());
            resp.put("success", true);
            resp.put("data", data);
            return resp;
        } catch (IOException e) {
            log.error("文件上传失败：fileName={}, 错误：{}", file.getOriginalFilename(), e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "文件上传失败：" + e.getMessage());
            throw e;
        }
    }
}
