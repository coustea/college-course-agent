package com.ccut.controller;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        if (file == null || file.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "文件为空");
            return resp;
        }

        // 基础上传目录：运行目录下 uploads/yyyy-MM-dd
        String dateDir = LocalDate.now().toString();
        Path uploadDir = Paths.get("uploads", dateDir);
        Files.createDirectories(uploadDir);

        // 生成安全文件名：uuid + 原始扩展名
        String original = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(ext)) {
            filename = filename + "." + ext.toLowerCase();
        }

        Path target = uploadDir.resolve(filename);
        file.transferTo(target.toFile());

        // 返回可直接访问的相对 URL，由静态资源映射 /uploads/** 提供
        String url = "/uploads/" + dateDir + "/" + filename;

        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        resp.put("success", true);
        resp.put("data", data);
        return resp;
    }
}


