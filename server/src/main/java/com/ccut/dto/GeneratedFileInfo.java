package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 生成文件信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedFileInfo {
    private String name; // 文件名
    private String url; // 前端可访问路径
    private String type; // 文件类型
    private String description; // 简短描述
    private Boolean generated; // 是否为 AI 生成文件
}
