package com.ccut.plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Chat 工具调用上下文
 *
 * <p>用于在一次对话请求内收集工具调用信息。</p>
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

    private static final class RequestContext {
        private final Map<String, String> usedTools = new LinkedHashMap<>();
    }
}