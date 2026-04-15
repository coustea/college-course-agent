package com.ccut.service.Impl;

import com.ccut.context.UserContext;
import com.ccut.dto.Attachment;
import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import com.ccut.dto.GeneratedFileInfo;
import com.ccut.entity.Message;
import com.ccut.entity.Student;
import com.ccut.plugin.ChatToolContext;
import com.ccut.service.ChatAgentService;
import com.ccut.service.DocumentAnalysisService;
import com.ccut.service.MessageService;
import com.ccut.service.StudentService;
import com.ccut.utils.ReadFileUtils;
import com.ccut.plugin.ChatPluginManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天 Agent 服务实现 — Spring AI Native Function Calling 架构
 *
 * <p>核心特性：</p>
 * <ul>
 *     <li>插件化架构：动态加载 {@link com.ccut.plugin.ToolPlugin}，工具故障不影响基础会话</li>
 *     <li>动态技能：通过 {@link com.ccut.plugin.ChatSkill} 注入 System Prompt 指令段落</li>
 *     <li>动态 System Prompt：根据用户角色（学生/教师）和用户上下文动态构建</li>
 *     <li>混合工具集：支持传统静态 Function 工具和新型插件化工具</li>
 * </ul>
 */
@Service
public class ChatAgentServiceImpl implements ChatAgentService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAgentServiceImpl.class);
    private static final int MAX_HISTORY_MESSAGES = 20;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 预构建的 ChatClient (System Prompt 和 Tools 在每次请求时动态设置) */
    private ChatClient chatClient;

    /** 基础系统提示词模板（不含用户上下文） */
    private final String baseSystemPrompt;

    @Autowired
    private MessageService messageService;

    @Autowired
    private DocumentAnalysisService documentAnalysisService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ChatPluginManager pluginManager;

    /** 传统静态注册的 ToolCallbacks (Spring Context 中的 Function beans) */
    @Autowired(required = false)
    private List<ToolCallback> staticToolCallbacks;

    @jakarta.annotation.PostConstruct
    public void init() {
        // 不再在 builder 中设置默认工具，改为在每次请求时动态注入
        this.chatClient = chatClientBuilder.build();
        logger.info("ChatClient 初始化完成，将使用动态工具注入模式");
    }

    public ChatAgentServiceImpl(
            @Value("classpath:prompts/system-prompt.md") Resource promptResource) {

        // 加载基础系统提示词模板
        String prompt;
        try {
            prompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("系统提示词文件加载失败，使用默认提示词: {}", e.getMessage());
            prompt = "你是一位智能教学助手，名字叫\"学小微\"。请详细、专业地回答用户问题。";
        }
        this.baseSystemPrompt = prompt;
    }

    // ======================== 公共 API ========================

    @Override
    public ChatResponse chat(ChatRequest request, String username, String role) {
        String conversationId = request.conversationId();
        String originalInput = request.message();

        logger.info("========== [Chat Start] ==========");

        // 1. 构建用户 Prompt（含附件上下文）
        String aiPrompt = buildAIPrompt(originalInput, request.attachments());

        // 2. 保存用户消息
        String filesJson = serializeAttachments(request.attachments());
        messageService.saveUserMessage(conversationId, originalInput, username, filesJson);

        // 3. 加载历史消息
        List<Message> history = messageService.loadConversationHistory(conversationId, username);
        List<org.springframework.ai.chat.messages.Message> historyMsgs = buildHistoryMessages(history);

        // 4. 构建动态 System Prompt (包含插件指令)
        String dynamicPrompt = buildDynamicSystemPrompt(username, role);

        // 5. 聚合所有可用工具 (静态工具 + 插件工具)
        List<ToolCallback> allTools = gatherAllTools();

        // 6. 调用 ChatClient
        String aiText;
        List<GeneratedFileInfo> generatedFiles;
        ChatToolContext.startRequest();
        try {
            aiText = chatClient.prompt()
                    .system(dynamicPrompt)
                    .messages(historyMsgs)
                    .user(aiPrompt)
                    .toolCallbacks(allTools.toArray(new ToolCallback[0])) // 动态注入工具回调
                    .call()
                    .content();
            generatedFiles = ChatToolContext.snapshotGeneratedFiles();
        } catch (Exception e) {
            logger.error("ChatClient 调用失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            aiText = "抱歉，AI 服务暂时出现问题，请稍后重试。错误信息：" + e.getMessage();
            generatedFiles = List.of();
        } finally {
            ChatToolContext.clear();
        }

        if (aiText == null || aiText.isBlank()) {
            aiText = "抱歉，未能获取到有效回复，请重新提问。";
        }
        aiText = normalizeAssistantContent(aiText, generatedFiles);

        // 7. 保存 AI 消息并刷新缓存
        Message aiMessage = messageService.saveAIMessage(
                conversationId,
                aiText,
                username,
                serializeGeneratedFiles(generatedFiles)
        );
        refreshCacheAsync(conversationId, username);

        ChatResponse response = new ChatResponse();
        response.setAiMessage(aiMessage);
        response.setConversationId(conversationId);
        response.setGeneratedFiles(generatedFiles);
        return response;
    }

    @Override
    public Flux<String> chatStream(ChatRequest request, String username, String role) {
        String conversationId = request.conversationId();
        String originalInput = request.message();

        logger.info("========== [Stream Chat Start] ==========");

        // 1. 构建用户 Prompt
        String aiPrompt = buildAIPrompt(originalInput, request.attachments());

        // 2. 保存用户消息
        try {
            String filesJson = serializeAttachments(request.attachments());
            messageService.saveUserMessage(conversationId, originalInput, username, filesJson);
        } catch (Exception e) {
            logger.error("保存用户消息失败: {}", e.getMessage(), e);
            return Flux.just("{\"code\":500,\"message\":\"保存消息失败\"}");
        }

        // 3. 加载历史消息
        List<Message> history;
        try {
            history = messageService.loadConversationHistory(conversationId, username);
        } catch (Exception e) {
            history = List.of();
        }
        List<org.springframework.ai.chat.messages.Message> historyMsgs = buildHistoryMessages(history);

        // 4. 构建动态 System Prompt
        String dynamicPrompt = buildDynamicSystemPrompt(username, role);

        // 5. 聚合所有可用工具
        List<ToolCallback> allTools = gatherAllTools();

        // 6. 调用 ChatClient (同步获取结果再分块，以确保 Tool Call 完整执行)
        String aiText;
        List<GeneratedFileInfo> generatedFiles;
        ChatToolContext.startRequest();
        try {
            aiText = chatClient.prompt()
                    .system(dynamicPrompt)
                    .messages(historyMsgs)
                    .user(aiPrompt)
                    .toolCallbacks(allTools.toArray(new ToolCallback[0])) // 动态注入工具回调
                    .call()
                    .content();
            generatedFiles = ChatToolContext.snapshotGeneratedFiles();
        } catch (Exception e) {
            logger.error("流式 ChatClient 调用失败: {}", e.getMessage(), e);
            return Flux.just("{\"code\":500,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        } finally {
            ChatToolContext.clear();
        }

        if (aiText == null || aiText.isBlank()) {
            aiText = "抱歉，未能获取到有效回复，请重新提问。";
        }
        aiText = normalizeAssistantContent(aiText, generatedFiles);

        // 7. 保存 AI 消息并刷新缓存
        try {
            messageService.saveAIMessage(
                    conversationId,
                    aiText,
                    username,
                    serializeGeneratedFiles(generatedFiles)
            );
            refreshCacheAsync(conversationId, username);
        } catch (Exception e) {
            logger.error("保存 AI 消息失败: {}", e.getMessage(), e);
        }

        // 8. 返回 SSE 分块
        final String chunkText = aiText;
        return Flux.create(sink -> {
            int chunkSize = 4;
            for (int i = 0; i < chunkText.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, chunkText.length());
                String chunk = chunkText.substring(i, end);
                sink.next(formatSseChunk(chunk));
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    sink.complete();
                    return;
                }
            }
            if (generatedFiles != null && !generatedFiles.isEmpty()) {
                sink.next(formatSseFiles(generatedFiles));
            }
            sink.complete();
        });
    }

    /**
     * 聚合所有可用工具：包含 Spring Context 中静态定义的 Function Tools 和 插件系统加载的 Tools。
     */
    private List<ToolCallback> gatherAllTools() {
        List<ToolCallback> tools = new ArrayList<>();
        // 1. 添加静态工具
        if (staticToolCallbacks != null) {
            tools.addAll(staticToolCallbacks);
        }
        // 2. 添加插件工具
        tools.addAll(pluginManager.getActiveToolCallbacks());
        return tools;
    }

    // ======================== 动态 System Prompt ========================

    /**
     * 构建动态 System Prompt：基础 prompt + 插件指令 + 用户上下文 + 角色差异化指引
     */
    private String buildDynamicSystemPrompt(String username, String role) {
        StringBuilder sb = new StringBuilder(baseSystemPrompt);

        String capabilityCatalog = pluginManager.getCapabilityCatalog();
        if (!capabilityCatalog.isBlank()) {
            sb.append(capabilityCatalog);
        }

        // 注入插件系统提供的动态指令（例如 Skill 定义的规则）
        String pluginInstructions = pluginManager.getDynamicInstructions();
        if (!pluginInstructions.isBlank()) {
            sb.append(pluginInstructions);
        }

        // 构建用户上下文块
        sb.append("\n\n## 当前用户上下文\n\n");
        sb.append("用户名：").append(username).append("\n");
        sb.append("角色：").append(role).append("\n");

        // 如果是学生，查询并注入学生 profile
        UserContext.Context ctx = UserContext.get();
        if ("student".equalsIgnoreCase(role) && ctx != null && ctx.userId() != null) {
            try {
                Student student = studentService.selectById(ctx.userId());
                if (student != null) {
                    sb.append("姓名：").append(student.getName() != null ? student.getName() : "").append("\n");
                    sb.append("学号：").append(student.getStudentNumber() != null ? student.getStudentNumber() : "").append("\n");
                    sb.append("班级：").append(student.getClassName() != null ? student.getClassName() : "").append("\n");
                    sb.append("专业：").append(student.getMajor() != null ? student.getMajor() : "").append("\n");
                    sb.append("年级：").append(student.getGrade() != null ? student.getGrade() : "").append("\n");
                }
            } catch (Exception e) {
                logger.warn("查询学生信息失败（不影响对话）: {}", e.getMessage());
            }
        }

        // 角色差异化行为指引
        if ("teacher".equalsIgnoreCase(role)) {
            sb.append("\n### 教师角色行为指引\n");
            sb.append("当前用户是**教师**，你的回答应侧重于：\n");
            sb.append("- 教学设计建议（教案优化、课堂互动方案、教学方法改进）\n");
            sb.append("- 学情分析和学生数据解读\n");
            sb.append("- 课程资源规划和教学内容推荐\n");
            sb.append("- 试卷命题和教学评估\n");
            sb.append("- 学生管理建议\n");
        } else {
            sb.append("\n### 学生角色行为指引\n");
            sb.append("当前用户是**学生**，你的回答应侧重于：\n");
            sb.append("- 学习指导和知识点讲解\n");
            sb.append("- 学习进度跟踪和学习建议\n");
            sb.append("- 错题分析和薄弱环节诊断\n");
            sb.append("- 考试备考策略和练习建议\n");
            sb.append("- 激励式引导和学法建议\n");
        }

        return sb.toString();
    }

    // ======================== Prompt 构建 ========================

    /**
     * 构建带附件上下文的 AI 输入 Prompt
     */
    private String buildAIPrompt(String originalInput, List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return originalInput != null ? originalInput : "";
        }

        StringBuilder aiPromptBuilder = new StringBuilder();
        boolean hasContent = false;

        // 解析文档附件内容
        String docContent = documentAnalysisService.analyzeAllAttachments(attachments);
        if (docContent != null && !docContent.isEmpty()) {
            aiPromptBuilder.append("<文档上下文>\n").append(docContent).append("\n</文档上下文>\n\n");
            hasContent = true;
        }

        // 解析图片附件内容（Base64 编码）
        String imageContext = buildImageDescriptions(attachments);
        if (imageContext != null && !imageContext.isEmpty()) {
            aiPromptBuilder.append("<图片上下文>\n").append(imageContext).append("\n</图片上下文>\n\n");
            hasContent = true;
        }

        // 无内容时直接返回原始输入
        if (!hasContent) {
            return originalInput != null ? originalInput : "";
        }

        // 构建用户提问
        String query = (originalInput != null && !originalInput.trim().isEmpty())
                ? originalInput
                : "请帮我分析上述提供的文件和图片内容，提取核心信息并进行总结。";

        aiPromptBuilder.append("<用户提问>\n").append(query).append("\n</用户提问>\n\n");
        aiPromptBuilder.append("【系统规则】请严格基于上述<文档上下文>和<图片上下文>的内容回答用户的提问。" +
                "如果资料中找不到答案，请说明。切勿在回答中大段重复照抄上下文的原文。");

        return aiPromptBuilder.toString();
    }

    /**
     * 构建图片附件的描述文本（Base64 编码）
     */
    private String buildImageDescriptions(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";
        List<Attachment> images = documentAnalysisService.getImageAttachments(attachments);
        if (images.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Attachment img : images) {
            try {
                String filePath = resolveFilePath(img.url());
                File file = new File(filePath);
                if (file.exists()) {
                    String ext = getExtension(img.filename());
                    String mimeType = ReadFileUtils.getImageMimeType(ext);
                    String base64 = ReadFileUtils.readImageAsBase64(file);
                    sb.append("图片文件: ").append(img.filename())
                            .append("\n![image](data:").append(mimeType)
                            .append(";base64,").append(base64).append(")\n");
                }
            } catch (Exception e) {
                logger.error("读取图片失败: {}", img.filename(), e);
            }
        }
        return sb.toString();
    }

    // ======================== 历史消息构建 ========================

    /**
     * 将数据库中的消息记录转换为 Spring AI 的 Message 列表
     */
    private List<org.springframework.ai.chat.messages.Message> buildHistoryMessages(List<Message> history) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);

        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);

            // 跳过最后一条用户消息（当前请求的新消息会单独添加）
            if (i == history.size() - 1 && "user".equals(msg.getRole())) {
                continue;
            }

            if ("user".equals(msg.getRole())) {
                String historyContent = msg.getContent();

                // 如果历史消息包含附件，重新解析附件内容作为上下文
                if (msg.getFiles() != null && !msg.getFiles().trim().isEmpty()
                        && !msg.getFiles().equals("[]") && !msg.getFiles().equals("null")) {
                    try {
                        List<Attachment> historyAttachments = objectMapper.readValue(
                                msg.getFiles(),
                                new TypeReference<List<Attachment>>() {}
                        );

                        String pastDocContent = documentAnalysisService.analyzeAllAttachments(historyAttachments);

                        if (pastDocContent != null && !pastDocContent.isEmpty()) {
                            historyContent = "【用户在此时上传了补充资料】\n<历史文档资料>\n"
                                    + pastDocContent
                                    + "\n</历史文档资料>\n"
                                    + "用户的历史提问：" + historyContent;
                        }
                    } catch (Exception e) {
                        logger.error("复原历史消息附件上下文失败: {}", e.getMessage());
                    }
                }
                messages.add(new UserMessage(historyContent));

            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
    }

    // ======================== 工具方法 ========================

    private String serializeAttachments(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (Exception e) {
            logger.error("附件JSON序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private String resolveFilePath(String url) {
        if (url == null) return "";
        if (url.startsWith("/uploads/")) return url.replace("/uploads/", "uploads/");
        return url;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String formatSseChunk(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"content\":\"" + escaped + "\"}";
    }

    private String formatSseFiles(List<GeneratedFileInfo> generatedFiles) {
        try {
            return objectMapper.writeValueAsString(Map.of("files", generatedFiles));
        } catch (Exception e) {
            logger.error("SSE 文件元数据序列化失败: {}", e.getMessage(), e);
            return "{\"files\":[]}";
        }
    }

    private String serializeGeneratedFiles(List<GeneratedFileInfo> generatedFiles) {
        if (generatedFiles == null || generatedFiles.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(generatedFiles);
        } catch (Exception e) {
            logger.error("生成文件元数据序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String normalizeAssistantContent(String aiText, List<GeneratedFileInfo> generatedFiles) {
        String normalized = aiText == null ? "" : aiText.replaceAll("\\[document:[^\\]]+\\]", "").trim();
        if (generatedFiles == null || generatedFiles.isEmpty()) {
            return normalized;
        }
        if (normalized.isBlank()) {
            return "已根据你的要求生成文件，见下方文件卡片。";
        }
        if (normalized.length() > 240) {
            return normalized.substring(0, 240).trim() + "\n\n已生成文件，见下方文件卡片。";
        }
        return normalized;
    }

    @Async("chatExecutor")
    protected void refreshCacheAsync(String conversationId, String username) {
        try {
            messageService.refreshConversationCache(conversationId, username);
        } catch (Exception e) {
            logger.error("刷新缓存失败: {}", e.getMessage(), e);
        }
    }
}
