package com.ccut.plugin;

import com.ccut.dto.GeneratedFileInfo;
import com.ccut.service.DocumentGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Word 文档生成工具
 */
@Component
public class WordGenerationPlugin implements ToolPlugin {

    @Autowired
    private DocumentGeneratorService documentGeneratorService;

    @Override
    public String getName() {
        return "generate_word_document";
    }

    @Override
    public String getDescription() {
        return "生成可下载的 Word 文档。当用户要求生成教案、报告、方案、总结、学习计划等正式文档时使用。" +
                "输入参数：title（文档标题）、contentDescription（完整正文或结构化内容）、description（可选，展示在文件卡片上的简短说明）。";
    }

    @Override
    public Class<?> getRequestType() {
        return WordDocumentRequest.class;
    }

    @Override
    public Object execute(Object request) {
        WordDocumentRequest req = (WordDocumentRequest) request;
        if (req.title() == null || req.title().isBlank()) {
            return "生成失败：未提供文档标题";
        }
        if (req.contentDescription() == null || req.contentDescription().isBlank()) {
            return "生成失败：未提供文档内容";
        }

        String fileUrl = documentGeneratorService.generateWord(req.title(), req.contentDescription());
        if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
            ChatToolContext.recordGeneratedFile(new GeneratedFileInfo(
                    req.title() + ".docx",
                    fileUrl,
                    "docx",
                    buildDescription(req.description(), req.contentDescription()),
                    true
            ));
            return "Word 文档已生成，系统会展示下载卡片。";
        }
        return fileUrl != null ? fileUrl : "Word 文档生成失败";
    }

    private String buildDescription(String description, String content) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "AI 生成的 Word 文档";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) + "..." : normalized;
    }

    public record WordDocumentRequest(
            String title,
            String contentDescription,
            String description
    ) {}
}
