package com.ccut.plugin;

import com.ccut.dto.GeneratedFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件生成插件 — 允许 AI Agent 为用户生成文档并提供下载卡片
 */
@Component
public class FileGenerationPlugin implements ToolPlugin {

    private static final Logger logger = LoggerFactory.getLogger(FileGenerationPlugin.class);

    @Value("${file.upload-dir:src/main/resources/uploads}")
    private String uploadDir;

    @Override
    public String getName() {
        return "generate_file";
    }

    @Override
    public String getDescription() {
        return "根据用户需求生成一个文本或 Markdown 文件，并提供下载链接。当用户要求你生成报告、总结、学习计划或导出文本资料为文件时使用。输入参数：filename（带后缀的文件名，如 plan.md, summary.txt），content（文件完整内容）。";
    }

    @Override
    public Class<?> getRequestType() {
        return FileGenerationRequest.class;
    }

    @Override
    public Object execute(Object request) {
        FileGenerationRequest req = (FileGenerationRequest) request;

        if (req.filename() == null || req.filename().isBlank()) {
            return "生成失败：未提供文件名";
        }
        if (req.content() == null || req.content().isBlank()) {
            return "生成失败：未提供文件内容";
        }

        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dir = Path.of(uploadDir, "generated", dateDir);
            Files.createDirectories(dir);

            String safeFilename = req.filename().replaceAll("[\\\\/:*?\"<>|]", "_");
            String uniqueFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + safeFilename;
            Path file = dir.resolve(uniqueFilename);

            Files.writeString(file, req.content(), StandardCharsets.UTF_8);

            String fileUrl = "/uploads/generated/" + dateDir + "/" + uniqueFilename;
            String type = safeFilename.contains(".")
                    ? safeFilename.substring(safeFilename.lastIndexOf('.') + 1).toLowerCase()
                    : "txt";

            logger.info("AI 成功生成文件: {}", file.toAbsolutePath());

            ChatToolContext.recordGeneratedFile(new GeneratedFileInfo(
                    safeFilename,
                    fileUrl,
                    type,
                    buildDescription(req.content()),
                    true
            ));

            return "文件已生成，系统会展示下载卡片。";

        } catch (Exception e) {
            logger.error("文件生成失败", e);
            return "生成失败：" + e.getMessage();
        }
    }

    private String buildDescription(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "AI 生成的文件";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) + "..." : normalized;
    }

    public record FileGenerationRequest(String filename, String content) {}
}
