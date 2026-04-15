package com.ccut.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件编辑插件 — 允许 AI Agent 编辑项目中的文件
 *
 * <p>支持两种编辑模式：</p>
 * <ul>
 *     <li>replace — 替换文件中匹配的文本片段</li>
 *     <li>write — 完整覆写文件内容（用于创建新文件或全面修改）</li>
 *     <li>insert — 在指定行号后插入内容</li>
 * </ul>
 *
 * <p>安全措施：只能在沙盒目录内操作</p>
 */
@Component
public class FileEditPlugin implements ToolPlugin {

    private static final Logger logger = LoggerFactory.getLogger(FileEditPlugin.class);
    private static final int MAX_FILE_SIZE = 500 * 1024; // 500KB

    @Value("${shell.sandbox-dir:#{systemProperties['user.dir']}}")
    private String sandboxDir;

    @Override
    public String getName() {
        return "edit_file";
    }

    @Override
    public String getDescription() {
        return "编辑或创建项目中的文件。支持三种模式：" +
                "1) replace：替换文件中匹配的文本（需提供 oldText 和 newText）；" +
                "2) write：完整覆写或创建文件（需提供 content）；" +
                "3) insert：在指定行号后插入内容（需提供 content 和 lineNumber）。" +
                "输入参数：filePath（文件路径），mode（replace/write/insert），" +
                "oldText（replace模式的原始文本），newText（replace模式的替换文本），" +
                "content（write/insert模式的内容），lineNumber（insert模式的行号）。";
    }

    @Override
    public Class<?> getRequestType() {
        return FileEditRequest.class;
    }

    @Override
    public Object execute(Object request) {
        FileEditRequest req = (FileEditRequest) request;

        if (req.filePath() == null || req.filePath().isBlank()) {
            return "{\"success\": false, \"error\": \"文件路径不能为空\"}";
        }
        if (req.mode() == null || req.mode().isBlank()) {
            return "{\"success\": false, \"error\": \"编辑模式不能为空（replace/write/insert）\"}";
        }

        Path resolvedPath = new File(sandboxDir).toPath().resolve(req.filePath()).normalize();

        // 安全检查
        Path sandboxPath = new File(sandboxDir).toPath().normalize();
        if (!resolvedPath.startsWith(sandboxPath)) {
            logger.warn("路径遍历攻击拦截: {} -> {}", req.filePath(), resolvedPath);
            return "{\"success\": false, \"error\": \"不允许操作项目目录外的文件\"}";
        }

        File file = resolvedPath.toFile();

        return switch (req.mode().toLowerCase()) {
            case "replace" -> doReplace(file, req);
            case "write" -> doWrite(file, req);
            case "insert" -> doInsert(file, req);
            default -> "{\"success\": false, \"error\": \"不支持的编辑模式: " + req.mode() + "，请使用 replace/write/insert\"}";
        };
    }

    private Object doReplace(File file, FileEditRequest req) {
        if (!file.exists()) {
            return "{\"success\": false, \"error\": \"文件不存在: " + req.filePath() + "\"}";
        }
        if (req.oldText() == null || req.oldText().isBlank()) {
            return "{\"success\": false, \"error\": \"replace 模式需要提供 oldText 参数\"}";
        }
        if (req.newText() == null) {
            return "{\"success\": false, \"error\": \"replace 模式需要提供 newText 参数\"}";
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            if (!content.contains(req.oldText())) {
                return "{\"success\": false, \"error\": \"在文件中未找到要替换的文本。请确保 oldText 与文件中的内容完全匹配。\"}";
            }

            // 检查是否有多处匹配
            int count = countOccurrences(content, req.oldText());
            if (count > 1 && !Boolean.TRUE.equals(req.replace_all())) {
                return "{\"success\": false, \"error\": \"文本在文件中出现 " + count + " 次。请提供更精确的上下文使其唯一匹配，或设置 replace_all 为 true 替换所有。\"}";
            }

            String newContent;
            if (Boolean.TRUE.equals(req.replace_all())) {
                newContent = content.replace(req.oldText(), req.newText());
            } else {
                newContent = content.replaceFirst(req.oldText().replace("$", "\\$").replace("(", "\\("), req.newText().replace("$", "\\$"));
            }

            Files.writeString(file.toPath(), newContent, StandardCharsets.UTF_8);
            logger.info("文件编辑成功(replace): {}, 替换 {} 处", req.filePath(), count);

            return "{\"success\": true, \"filePath\": \"" + escapeJson(req.filePath()) +
                    "\", \"mode\": \"replace\", \"replacements\": " + (Boolean.TRUE.equals(req.replace_all()) ? count : 1) + "}";

        } catch (Exception e) {
            logger.error("文件编辑失败(replace): {} - {}", req.filePath(), e.getMessage());
            return "{\"success\": false, \"error\": \"编辑文件失败: " + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private Object doWrite(File file, FileEditRequest req) {
        if (req.content() == null) {
            return "{\"success\": false, \"error\": \"write 模式需要提供 content 参数\"}";
        }

        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Files.writeString(file.toPath(), req.content(), StandardCharsets.UTF_8);
            logger.info("文件写入成功(write): {}, {} 字节", req.filePath(), req.content().length());

            return "{\"success\": true, \"filePath\": \"" + escapeJson(req.filePath()) +
                    "\", \"mode\": \"write\", \"size\": " + req.content().length() + "}";

        } catch (Exception e) {
            logger.error("文件写入失败(write): {} - {}", req.filePath(), e.getMessage());
            return "{\"success\": false, \"error\": \"写入文件失败: " + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private Object doInsert(File file, FileEditRequest req) {
        if (req.content() == null || req.content().isBlank()) {
            return "{\"success\": false, \"error\": \"insert 模式需要提供 content 参数\"}";
        }
        if (req.lineNumber() == null || req.lineNumber() < 0) {
            return "{\"success\": false, \"error\": \"insert 模式需要提供有效的 lineNumber 参数\"}";
        }

        try {
            List<String> lines;
            if (file.exists()) {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            } else {
                lines = List.of();
            }

            int insertAt = Math.min(req.lineNumber(), lines.size());
            lines = new java.util.ArrayList<>(lines);

            // 在指定行号后插入
            String[] insertLines = req.content().split("\n");
            for (int i = 0; i < insertLines.length; i++) {
                lines.add(insertAt + i, insertLines[i]);
            }

            Files.writeString(file.toPath(), String.join("\n", lines), StandardCharsets.UTF_8);
            logger.info("文件编辑成功(insert): {}, 在第 {} 行后插入 {} 行", req.filePath(), req.lineNumber(), insertLines.length);

            return "{\"success\": true, \"filePath\": \"" + escapeJson(req.filePath()) +
                    "\", \"mode\": \"insert\", \"insertedAt\": " + insertAt +
                    ", \"insertedLines\": " + insertLines.length + "}";

        } catch (Exception e) {
            logger.error("文件编辑失败(insert): {} - {}", req.filePath(), e.getMessage());
            return "{\"success\": false, \"error\": \"插入内容失败: " + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private int countOccurrences(String text, String search) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(search, idx)) != -1) {
            count++;
            idx += search.length();
        }
        return count;
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

    public record FileEditRequest(
            String filePath,
            String mode,
            String oldText,
            String newText,
            String content,
            Integer lineNumber,
            Boolean replace_all
    ) {}
}
