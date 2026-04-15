package com.ccut.context;

import com.ccut.entity.User;

/**
 * 线程级用户上下文 — 供 Agent 工具在执行时获取当前用户身份。
 */
public final class UserContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(Long userId, String username, User.Role role) {
        HOLDER.set(new Context(userId, username, role));
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record Context(Long userId, String username, User.Role role) {}
}
