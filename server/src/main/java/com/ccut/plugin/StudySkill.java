package com.ccut.plugin;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 学习辅助技能示例
 */
@Component
public class StudySkill implements ChatSkill {

    @Override
    public String getSkillName() {
        return "学习方法强化技能";
    }

    @Override
    public String getSkillDescription() {
        return "为学生提供更专业的学习方法指引";
    }

    @Override
    public String getSkillInstruction() {
        return "当你发现学生在某个知识点遇到困难时，除了直接解答，还应提供一种相关的学习方法（如费曼技巧、番茄工作法、错题分类法等），帮助学生授人以鱼不如授人以渔。";
    }

    @Override
    public List<String> getToolNames() {
        return List.of("recommend_courses");
    }
}
