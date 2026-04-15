package com.ccut.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件读取插件 — 允许 AI Agent 读取项目中的文件内容
 *
 * <p>安全措施：</p>
 * <ul>
 *     <li>只能读取沙盒目录内的文件</li>
 *     <li>限制单次读取最大 100KB</li>
 *     <li>支持指定行范围读取（startLine, endLine）</li>
 * </ul>
 */
@Component
public class FileReaderPlugin implements ToolPlugin {

    private static final Logger logger = LoggerFactory.getLogger(FileReaderPlugin.class);
    private static final int MAX_FILE_SIZE = 100 * 1024; // 100KB
    private static final int MAX_LINE_COUNT = 500;

    @Value("${shell.sandbox-dir:#{systemProperties['user.dir']}}")
    private String sandboxDir;

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public String getDescription() {
        return "读取指定文件的内容。可以读取代码文件、配置文件、文本文件等。" +
                "支持通过 startLine 和 endLine 参数读取指定行范围。" +
                "输入参数：filePath（文件路径，相对于项目根目录），startLine（可选，起始行号，从1开始），" +
                "endLine（可选，结束行号）。";
    }

    @Override
    public Class<?> getRequestType() {
        return FileReadRequest.class;
    }

    @Override
    public Object execute(Object request) {
        FileReadRequest req = (FileReadRequest) request;

        if (req.filePath() == null || req.filePath().isBlank()) {
            return "{\"success\": false, \"error\": \"文件路径不能为空\"}";
        }

        Path resolvedPath = new File(sandboxDir).toPath().resolve(req.filePath()).normalize();

        // 安全检查：确保路径在沙盒目录内
        Path sandboxPath = new File(sandboxDir).toPath().normalize();
        if (!resolvedPath.startsWith(sandboxPath)) {
            logger.warn("路径遍历攻击拦截: {} -> {}", req.filePath(), resolvedPath);
            return "{\"success\": false, \"error\": \"不允许访问项目目录外的文件\"}";
        }

        File file = resolvedPath.toFile();
        if (!file.exists()) {
            return "{\"success\": false, \"error\": \"文件不存在: " + req.filePath() + "\"}";
        }
        if (!file.isFile()) {
            return "{\"success\": false, \"error\": \"路径不是文件: " + req.filePath() + "\"}";
        }
        if (!file.canRead()) {
            return "{\"success\": false, \"error\": \"文件不可读: " + req.filePath() + "\"}";
        }
        if (file.length() > MAX_FILE_SIZE) {
            return "{\"success\": false, \"error\": \"文件过大（" + (file.length() / 1024) + "KB），最大允许 100KB。请使用 startLine/endLine 参数分段读取。\"}";
        }

        try {
            String content = Files.readString(resolvedPath, StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            int totalLines = lines.length;

            // 处理行范围
            int startLine = req.startLine() != null ? Math.max(1, req.startLine()) : 1;
            int endLine = req.endLine() != null ? Math.min(totalLines, req.endLine()) : totalLines;

            // 限制最大行数
            if (endLine - startLine + 1 > MAX_LINE_COUNT) {
                endLine = startLine + MAX_LINE_COUNT - 1;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = startLine - 1; i < endLine && i < totalLines; i++) {
                sb.append(i + 1).append("\t").append(lines[i]);
                if (i < endLine - 1) sb.append("\n");
            }

            String result = sb.toString();
            boolean truncated = endLine < totalLines;

            return "{\"success\": true, \"filePath\": \"" + escapeJson(req.filePath()) +
                    "\", \"totalLines\": " + totalLines +
                    ", \"startLine\": " + startLine +
                    ", \"endLine\": " + endLine +
                    ", \"truncated\": " + truncated +
                    ", \"content\": \"" + escapeJson(result) + "\"}";

        } catch (Exception e) {
            logger.error("文件读取失败: {} - {}", req.filePath(), e.getMessage());
            return "{\"success\": false, \"error\": \"读取文件失败: " + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record FileReadRequest(
            String filePath,
            Integer startLine,
            Integer endLine
    ) {}
}
