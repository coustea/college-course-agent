package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileInfo {

    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
}