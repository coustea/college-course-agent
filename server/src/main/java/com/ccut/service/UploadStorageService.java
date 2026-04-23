package com.ccut.service;

import com.ccut.dto.StoredUpload;
import org.springframework.web.multipart.MultipartFile;

/**
 * 统一管理本地上传文件的保存路径、访问 URL 和删除边界。
 */
public interface UploadStorageService {

    default StoredUpload store(MultipartFile file) {
        return store(file, "");
    }

    StoredUpload store(MultipartFile file, String urlDirectory);

    boolean deleteByUrl(String fileUrl);
}
