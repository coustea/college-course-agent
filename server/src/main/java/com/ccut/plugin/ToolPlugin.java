package com.ccut.plugin;

import java.util.Map;

/**
 * AI 智能体工具插件接口 — 实现此接口的类将自动注册为大模型可调用的工具。
 *
 * <p>核心目标：插件化、解耦。工具内部发生错误或被删除，均不影响 ChatAgent 的基础会话流程。</p>
 */
public interface ToolPlugin {

    /**
     * 工具在 LLM 中的注册名称（唯一，符合 snake_case）
     */
    String getName();

    /**
     * 工具的描述信息，帮助 LLM 判断何时调用。
     */
    String getDescription();

    /**
     * 请求参数的 Class 类型。Spring AI 会据此进行 JSON 反序列化。
     */
    Class<?> getRequestType();

    /**
     * 工具的执行逻辑。
     *
     * @param request 已经由 Spring AI 反序列化好的请求对象
     * @return 执行结果（通常为 JSON 字符串或纯文本，LLM 能够理解的内容）
     */
    Object execute(Object request);

    /**
     * 是否启用。禁用后 LLM 将不再可见此工具。
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 获取工具组名。
     */
    default String getGroupName() {
        return "default";
    }
}
