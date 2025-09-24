package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@AllArgsConstructor
public class FileInfo {

    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
}
