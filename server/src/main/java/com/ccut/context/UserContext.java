package com.ccut.context;

import com.ccut.entity.User;

/**
 * 线程级用户上下文 — 供 Agent 工具在执行时获取当前用户身份。
 *
 * <p>Controller 在调用 Service 前通过 {@link #set(Long, String, User.Role)} 设置，
 * finally 中通过 {@link #clear()} 清理，防止 ThreadLocal 泄漏。</p>
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
