package com.ccut.plugin;

import com.ccut.dto.GeneratedFileInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat 工具调用上下文
 *
 * <p>用于在一次对话请求内收集工具调用产生的结构化结果，例如生成文件。</p>
 */
public final class ChatToolContext {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private ChatToolContext() {
    }

    public static void startRequest() {
        CONTEXT.set(new RequestContext());
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void recordTool(String toolName) {
        RequestContext ctx = CONTEXT.get();
        if (ctx != null && toolName != null && !toolName.isBlank()) {
            ctx.usedTools.put(toolName, toolName);
        }
    }

    public static void recordGeneratedFile(GeneratedFileInfo fileInfo) {
        RequestContext ctx = CONTEXT.get();
        if (ctx == null || fileInfo == null || fileInfo.getUrl() == null || fileInfo.getUrl().isBlank()) {
            return;
        }
        if (fileInfo.getGenerated() == null) {
            fileInfo.setGenerated(true);
        }
        ctx.generatedFiles.put(fileInfo.getUrl(), fileInfo);
    }

    public static List<GeneratedFileInfo> snapshotGeneratedFiles() {
        RequestContext ctx = CONTEXT.get();
        if (ctx == null || ctx.generatedFiles.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(ctx.generatedFiles.values());
    }

    private static final class RequestContext {
        private final Map<String, String> usedTools = new LinkedHashMap<>();
        private final Map<String, GeneratedFileInfo> generatedFiles = new LinkedHashMap<>();
    }
}
