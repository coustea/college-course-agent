package com.ccut.service;

import com.ccut.dto.Attachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {

    /**
     * 保存单个文件并返回附件信息
     * @param file 上传的文件
     * @return 附件信息（包含URL）
     */
    Attachment saveFile(MultipartFile file);

    /**
     * 批量保存文件并返回附件信息列表
     * @param files 上传的文件列表
     * @return 附件信息列表
     */
    List<Attachment> saveFiles(List<MultipartFile> files);

    /**
     * 根据文件名判断文件类型
     * @param filename 文件名
     * @return 文件类型：image, document, video, other
     */
    String getFileType(String filename);

    /**
     * 删除文件
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);
}