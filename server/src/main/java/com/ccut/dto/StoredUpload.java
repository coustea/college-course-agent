package com.ccut.dto;

import java.nio.file.Path;

/**
 * 已保存上传文件的信息。
 */
public record StoredUpload(
        String originalFilename,
        String savedFilename,
        String url,
        Path path,
        Long size,
        String mimeType
) {}
