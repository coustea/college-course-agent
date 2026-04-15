package com.ccut.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能体插件管理器 — 统一管理所有 {@link ToolPlugin} 和 {@link ChatSkill}。
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>自动发现：扫描 Spring Context 里的 Java Bean 插件。</li>
 *     <li>文件加载：扫描 src/main/resources/skills/ 目录下的 .md 文件作为技能加载。</li>
 *     <li>容错执行：将 {@link ToolPlugin} 包装为 Spring AI 的 {@link ToolCallback}，并内置错误捕获。</li>
 *     <li>动态注入：在会话发起时提供当前可用的工具集和系统指令。</li>
 * </ul>
 */
@Service
public class ChatPluginManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatPluginManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ApplicationContext applicationContext;

    /** 所有注册的工具 (Bean名称 -> 插件对象) */
    private final Map<String, ToolPlugin> toolPlugins = new ConcurrentHashMap<>();

    /** 所有注册的技能 (技能名称/文件路径 -> 技能对象) */
    private final Map<String, ChatSkill> chatSkills = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshPlugins();
    }

    /**
     * 手动刷新插件列表。
     * 支持从 Spring Context 扫描 Bean，同时从 resources/skills/ 目录加载 Markdown 技能文件。
     */
    public synchronized void refreshPlugins() {
        // 1. 发现 ToolPlugin (Java Bean)
        Map<String, ToolPlugin> discoveredTools = applicationContext.getBeansOfType(ToolPlugin.class);
        toolPlugins.clear();
        toolPlugins.putAll(discoveredTools);

        // 2. 发现 ChatSkill (Java Bean)
        Map<String, ChatSkill> discoveredSkills = applicationContext.getBeansOfType(ChatSkill.class);
        chatSkills.clear();
        chatSkills.putAll(discoveredSkills);

        // 3. 加载文件类技能 (Resources Skills)
        loadFileBasedSkills();

        logger.info("Chat 插件刷新成功：发现 {} 个工具, {} 个技能 (目录格式 + 扁平格式)", toolPlugins.size(), chatSkills.size());
    }

    /**
     * 扫描 skills 目录下的技能定义。
     *
     * <p>支持两种格式（优先级从高到低）：</p>
     * <ol>
     *     <li><b>SKILL.md 标准格式</b>：{@code skills/{name}/SKILL.md}（推荐，每个技能一个目录）</li>
     *     <li><b>扁平格式</b>：{@code skills/{name}.md}（向后兼容）</li>
     * </ol>
     */
    private void loadFileBasedSkills() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 1. 扫描 SKILL.md 标准目录格式: skills/*/SKILL.md
        try {
            Resource[] dirResources = resolver.getResources("classpath*:skills/*/SKILL.md");
            for (Resource resource : dirResources) {
                try {
                    String skillName = extractSkillNameFromPath(resource, "/SKILL.md");
                    if (skillName == null) continue;

                    String content = resource.getContentAsString(StandardCharsets.UTF_8);
                    // 去掉 YAML frontmatter（--- ... ---），只保留指令正文
                    content = stripYamlFrontmatter(content);

                    ChatSkill fileSkill = new FileBasedChatSkill(skillName, content);
                    chatSkills.put("file:" + skillName, fileSkill);
                    logger.info("从目录加载技能: {}", skillName);
                } catch (IOException e) {
                    logger.error("加载技能文件失败: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            logger.debug("未找到 SKILL.md 格式技能: {}", e.getMessage());
        }

        // 2. 扫描扁平格式: skills/*.md（向后兼容）
        // 注意：这个模式也会匹配 skills/*/SKILL.md，所以需要跳过 SKILL.md
        try {
            Resource[] flatResources = resolver.getResources("classpath*:skills/*.md");
            for (Resource resource : flatResources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null) continue;

                    // 跳过目录格式中的 SKILL.md 文件（它们已被上面的循环处理）
                    if ("SKILL.md".equals(filename)) continue;

                    String skillName = filename.replace(".md", "");
                    // 如果已通过目录格式加载，跳过
                    if (chatSkills.containsKey("file:" + skillName)) continue;

                    String content = resource.getContentAsString(StandardCharsets.UTF_8);
                    content = stripYamlFrontmatter(content);

                    ChatSkill fileSkill = new FileBasedChatSkill(skillName, content);
                    chatSkills.put("file:" + skillName, fileSkill);
                    logger.info("从文件加载技能: {}", skillName);
                } catch (IOException e) {
                    logger.error("加载技能文件失败: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            logger.debug("未找到扁平格式技能: {}", e.getMessage());
        }
    }

    /**
     * 从资源路径中提取技能名称。
     * 例如 "skills/knowledge-explainer/SKILL.md" → "knowledge-explainer"
     */
    private String extractSkillNameFromPath(Resource resource, String suffix) {
        try {
            String uri = resource.getURI().toString();
            int skillsIdx = uri.lastIndexOf("skills/");
            if (skillsIdx < 0) return null;
            String afterSkills = uri.substring(skillsIdx + "skills/".length());
            int slashIdx = afterSkills.indexOf('/');
            if (slashIdx < 0) return null;
            return afterSkills.substring(0, slashIdx);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 去掉 YAML frontmatter（以 --- 包裹的头部），只保留指令正文。
     */
    private String stripYamlFrontmatter(String content) {
        if (content == null || !content.startsWith("---")) return content;
        int endIndex = content.indexOf("---", 3);
        if (endIndex < 0) return content;
        return content.substring(endIndex + 3).trim();
    }

    /**
     * 获取所有可用的工具回调列表
     */
    public List<ToolCallback> getActiveToolCallbacks() {
        return toolPlugins.values().stream()
                .filter(ToolPlugin::isEnabled)
                .map(ToolPluginWrapper::new)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有激活技能的 System Prompt 指令扩展。
     */
    public String getDynamicInstructions() {
        StringBuilder sb = new StringBuilder();
        List<ChatSkill> activeSkills = chatSkills.values().stream()
                .filter(ChatSkill::isActive)
                .collect(Collectors.toList());

        if (activeSkills.isEmpty()) return "";

        sb.append("\n\n## 激活的增强技能\n");
        for (ChatSkill skill : activeSkills) {
            String instruction = skill.getSkillInstruction();
            if (instruction != null && !instruction.isBlank()) {
                sb.append("### ").append(skill.getSkillName()).append("\n");
                sb.append(instruction).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取当前启用的工具 / 技能摘要，用于动态注入 System Prompt。
     */
    public String getCapabilityCatalog() {
        StringBuilder sb = new StringBuilder();

        List<ToolPlugin> activeTools = toolPlugins.values().stream()
                .filter(ToolPlugin::isEnabled)
                .sorted(Comparator.comparing(ToolPlugin::getName))
                .toList();
        if (!activeTools.isEmpty()) {
            sb.append("\n\n## 当前启用的 Tools\n");
            for (ToolPlugin tool : activeTools) {
                sb.append("- `").append(tool.getName()).append("`：")
                        .append(tool.getDescription()).append("\n");
            }
        }

        List<ChatSkill> activeSkills = chatSkills.values().stream()
                .filter(ChatSkill::isActive)
                .sorted(Comparator.comparing(ChatSkill::getSkillName))
                .toList();
        if (!activeSkills.isEmpty()) {
            sb.append("\n## 当前启用的 Skills\n");
            for (ChatSkill skill : activeSkills) {
                sb.append("- `").append(skill.getSkillName()).append("`：")
                        .append(skill.getSkillDescription());
                if (!skill.getToolNames().isEmpty()) {
                    sb.append("；关联工具：").append(String.join(", ", skill.getToolNames()));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 内部实现：基于文件的动态技能
     */
    private static class FileBasedChatSkill implements ChatSkill {
        private final String name;
        private final String instruction;

        public FileBasedChatSkill(String name, String instruction) {
            this.name = name;
            this.instruction = instruction;
        }

        @Override
        public String getSkillName() {
            return name;
        }

        @Override
        public String getSkillDescription() {
            return "从 Markdown 文件加载的动态技能";
        }

        @Override
        public String getSkillInstruction() {
            return instruction;
        }
    }

    /**
     * 内部包装类：将 {@link ToolPlugin} 转换为 Spring AI {@link ToolCallback}。
     */
    private static class ToolPluginWrapper implements ToolCallback {
        private final ToolPlugin plugin;
        private final ToolDefinition definition;

        public ToolPluginWrapper(ToolPlugin plugin) {
            this.plugin = plugin;
            this.definition = ToolDefinition.builder()
                    .name(plugin.getName())
                    .description(plugin.getDescription())
                    .inputSchema(generateJsonSchema(plugin.getRequestType()))
                    .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public String call(String jsonInput) {
            try {
                logger.debug("正在调用工具 [{}]，输入参数: {}", plugin.getName(), jsonInput);
                ChatToolContext.recordTool(plugin.getName());
                Object request = deserialize(jsonInput, plugin.getRequestType());
                Object result = plugin.execute(request);
                return serialize(result);
            } catch (Exception e) {
                logger.error("工具 [{}] 执行失败: {}", plugin.getName(), e.getMessage(), e);
                return "{\"error\": \"工具执行内部错误: " + e.getMessage() + "\", \"success\": false}";
            }
        }

        private String generateJsonSchema(Class<?> type) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            Map<String, Object> properties = new LinkedHashMap<>();
            schema.put("properties", properties);

            if (type == null) {
                return "{\"type\":\"object\",\"properties\":{}}";
            }

            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    properties.put(component.getName(), propertySchema(component.getType()));
                }
            } else {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    properties.put(field.getName(), propertySchema(field.getType()));
                }
            }

            try {
                return objectMapper.writeValueAsString(schema);
            } catch (JsonProcessingException e) {
                logger.warn("生成工具 [{}] 的 JSON Schema 失败，回退为空对象: {}", plugin.getName(), e.getMessage());
                return "{\"type\":\"object\",\"properties\":{}}";
            }
        }

        private Map<String, Object> propertySchema(Class<?> fieldType) {
            Map<String, Object> schema = new LinkedHashMap<>();
            if (fieldType == String.class || fieldType.isEnum()) {
                schema.put("type", "string");
            } else if (fieldType == Integer.class || fieldType == int.class
                    || fieldType == Long.class || fieldType == long.class
                    || fieldType == Short.class || fieldType == short.class) {
                schema.put("type", "integer");
            } else if (fieldType == Double.class || fieldType == double.class
                    || fieldType == Float.class || fieldType == float.class) {
                schema.put("type", "number");
            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                schema.put("type", "boolean");
            } else if (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType)) {
                schema.put("type", "array");
            } else {
                schema.put("type", "object");
            }
            return schema;
        }

        private Object deserialize(String json, Class<?> type) throws Exception {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, type);
        }

        private String serialize(Object data) throws Exception {
            if (data instanceof String) return (String) data;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(data);
        }
    }
}
