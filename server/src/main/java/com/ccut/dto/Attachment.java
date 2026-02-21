package com.ccut.dto;

/**
 * 附件信息 DTO
 */
public record Attachment(
    /**
     * 附件类型：image, document, video, other
     */
    String type,

    /**
     * 文件名
     */
    String filename,

    /**
     * 文件URL（已上传后的访问地址）
     */
    String url,

    /**
     * 文件大小（字节）
     */
    Long size,

    /**
     * MIME类型
     */
    String mimeType
) {}