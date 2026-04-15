package com.ccut.plugin;

import java.util.Collections;
import java.util.List;

/**
 * AI 智能体“技能”接口 — 技能是工具和指令的更高层级封装。
 *
 * <p>技能（Skill）可以包含：</p>
 * <ul>
 *     <li>特定的一组 {@link ToolPlugin}。</li>
 *     <li>一段注入到 System Prompt 中的特定指令（Prompt Segment）。</li>
 * </ul>
 */
public interface ChatSkill {

    /**
     * 技能名称。
     */
    String getSkillName();

    /**
     * 技能描述（对内部系统使用）。
     */
    String getSkillDescription();

    /**
     * 此技能特有的 System Prompt 指令段落。
     * 将在会话时动态注入到系统提示词中。
     */
    default String getSkillInstruction() {
        return "";
    }

    /**
     * 此技能关联的工具名称列表。
     */
    default List<String> getToolNames() {
        return Collections.emptyList();
    }

    /**
     * 技能是否处于激活状态。
     */
    default boolean isActive() {
        return true;
    }
}
