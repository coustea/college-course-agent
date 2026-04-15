package com.ccut.plugin;

import com.ccut.dto.GeneratedFileInfo;
import com.ccut.service.DocumentGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Excel 文档生成工具
 */
@Component
public class ExcelGenerationPlugin implements ToolPlugin {

    @Autowired
    private DocumentGeneratorService documentGeneratorService;

    @Override
    public String getName() {
        return "generate_excel_document";
    }

    @Override
    public String getDescription() {
        return "生成可下载的 Excel 表格。当用户要求生成成绩表、统计表、排课表、清单等表格文件时使用。" +
                "输入参数：title（表格标题）、dataDescription（表头和数据的结构化描述）、description（可选，展示在文件卡片上的简短说明）。";
    }

    @Override
    public Class<?> getRequestType() {
        return ExcelDocumentRequest.class;
    }

    @Override
    public Object execute(Object request) {
        ExcelDocumentRequest req = (ExcelDocumentRequest) request;
        if (req.title() == null || req.title().isBlank()) {
            return "生成失败：未提供表格标题";
        }
        if (req.dataDescription() == null || req.dataDescription().isBlank()) {
            return "生成失败：未提供表格数据描述";
        }

        String fileUrl = documentGeneratorService.generateExcel(req.title(), req.dataDescription());
        if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
            ChatToolContext.recordGeneratedFile(new GeneratedFileInfo(
                    req.title() + ".xlsx",
                    fileUrl,
                    "xlsx",
                    buildDescription(req.description(), req.dataDescription()),
                    true
            ));
            return "Excel 文件已生成，系统会展示下载卡片。";
        }
        return fileUrl != null ? fileUrl : "Excel 文件生成失败";
    }

    private String buildDescription(String description, String content) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "AI 生成的 Excel 表格";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) + "..." : normalized;
    }

    public record ExcelDocumentRequest(
            String title,
            String dataDescription,
            String description
    ) {}
}
