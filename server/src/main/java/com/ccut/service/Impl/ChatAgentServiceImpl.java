package com.ccut.service.Impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ccut.dto.Attachment;
import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import com.ccut.entity.Message;
import com.ccut.service.ChatAgentService;
import com.ccut.service.DocumentAnalysisService;
import com.ccut.service.MessageService;
import com.ccut.service.WebSearchService;
import com.ccut.utils.ReadFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
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

/**
 * AI 聊天 Agent 实现
 * 核心工作流：Think（思考）→ Plan（规划）→ Reflect（反思）→ Reply（回复）
 * 支持：联网搜索（ReAct）、图片理解、文档分析
 */
@Service
public class ChatAgentServiceImpl implements ChatAgentService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAgentServiceImpl.class);
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_REACT_ITERATIONS = 3;

    private final String agentPrompt;
    private final String directChatPrompt;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebSearchService webSearchService;

    @Autowired
    private DocumentAnalysisService documentAnalysisService;

    public ChatAgentServiceImpl(
            @Value("classpath:prompts/react-agent-prompt.md") Resource reactPromptResource,
            @Value("classpath:prompts/direct-chat-prompt.md") Resource directPromptResource) {
        String reactPrompt;
        String directPrompt;
        try {
            reactPrompt = reactPromptResource.getContentAsString(StandardCharsets.UTF_8);
            logger.info("已加载 ReAct Agent 提示词（{}字符）", reactPrompt.length());
        } catch (IOException e) {
            logger.warn("无法加载 ReAct Agent 提示词: {}", e.getMessage());
            reactPrompt = "你是一个智能教学助手\"学小微\"。请用友好、专业的方式回答问题。输出JSON：{\"thought\":\"...\",\"action\":\"answer\",\"input\":\"...\"}";
        }
        this.agentPrompt = reactPrompt;

        try {
            directPrompt = directPromptResource.getContentAsString(StandardCharsets.UTF_8);
            logger.info("已加载 Direct Chat 提示词（{}字符）", directPrompt.length());
        } catch (IOException e) {
            logger.warn("无法加载 Direct Chat 提示词: {}", e.getMessage());
            directPrompt = "你是一位智能教学助手，名字叫\"学小微\"。请直接、友好地回答用户的问题，使用 Markdown 格式。";
        }
        this.directChatPrompt = directPrompt;
    }

    // ============================================================
    //  公开接口实现
    // ============================================================

    @Override
    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.conversationId();
        String userInput = request.message();
        String username = extractUsername(conversationId);

        logger.info("Agent chat: conversationId={}", conversationId);

        // 1. 异步解析文档附件
        String docContent = documentAnalysisService.analyzeAllAttachments(request.attachments());
        if (!docContent.isEmpty()) {
            userInput += docContent;
        }

        // 2. 处理图片附件（嵌入 base64 描述）
        userInput = appendImageDescriptions(userInput, request.attachments());

        // 3. 保存用户消息
        messageService.saveUserMessage(conversationId, userInput, username);

        // 4. 加载历史消息
        List<Message> history = messageService.loadConversationHistory(conversationId, username);
        List<org.springframework.ai.chat.messages.Message> historyMsgs = buildHistoryMessages(history);

        // 5. Agent 执行：Think → Plan → Reflect → Reply
        String aiText;
        if (needsSearch(userInput)) {
            aiText = executeReActLoop(historyMsgs, userInput);
        } else {
            aiText = executeDirectChat(historyMsgs, userInput);
        }

        // 6. 保存 AI 回复
        Message aiMessage = messageService.saveAIMessage(conversationId, aiText, username);

        // 7. 异步刷新缓存
        refreshCacheAsync(conversationId, username);

        ChatResponse response = new ChatResponse();
        response.setAiMessage(aiMessage);
        response.setConversationId(conversationId);
        return response;
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String conversationId = request.conversationId();
        String userInput = request.message();
        String username = extractUsername(conversationId);

        logger.info("========== [Stream Chat Start] ==========");
        logger.info("ConversationId: {}", conversationId);
        logger.info("Username: {}", username);
        logger.info("User Input: {}", userInput);
        logger.info("Attachments: {}", request.attachments() != null ? request.attachments().size() : 0);

        // 1. 异步解析文档附件
        String docContent = documentAnalysisService.analyzeAllAttachments(request.attachments());
        if (!docContent.isEmpty()) {
            userInput += docContent;
            logger.info("文档内容已追加，总长度: {}", userInput.length());
        }

        // 2. 处理图片附件
        userInput = appendImageDescriptions(userInput, request.attachments());

        // 3. 保存用户消息
        try {
            messageService.saveUserMessage(conversationId, userInput, username);
            logger.info("用户消息已保存");
        } catch (Exception e) {
            logger.error("保存用户消息失败: {}", e.getMessage(), e);
            return Flux.just("{\"code\":500,\"message\":\"保存消息失败: " + escapeJson(e.getMessage()) + "\"}");
        }

        // 4. 加载历史
        List<Message> history;
        try {
            history = messageService.loadConversationHistory(conversationId, username);
            logger.info("加载历史消息: {} 条", history.size());
        } catch (Exception e) {
            logger.error("加载历史失败: {}", e.getMessage(), e);
            history = List.of();
        }
        List<org.springframework.ai.chat.messages.Message> historyMsgs = buildHistoryMessages(history);

        // 5. 收集完整回复
        StringBuilder fullResponse = new StringBuilder();

        if (needsSearch(userInput)) {
            logger.info("使用 ReAct 模式（需要搜索）");
            // ReAct Agent 流式（先完成搜索循环，再流式输出结果）
            final String finalInput = userInput;
            return Flux.create(sink -> {
                new Thread(() -> {
                    try {
                        logger.info("[ReAct] 开始执行搜索循环");
                        String answer = executeReActLoop(historyMsgs, finalInput);
                        logger.info("[ReAct] 搜索完成，回答长度: {}", answer.length());

                        // 逐块发送模拟流式效果
                        int chunkSize = 4;
                        int chunkCount = 0;
                        for (int i = 0; i < answer.length(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, answer.length());
                            String chunk = answer.substring(i, end);
                            fullResponse.append(chunk);
                            chunkCount++;
                            if (chunkCount % 50 == 0) {
                                logger.info("[ReAct] 已发送 {} 个chunk，累计 {} 字符", chunkCount, fullResponse.length());
                            }
                            sink.next("{\"content\":\"" + escapeJson(chunk) + "\"}");
                            Thread.sleep(15);
                        }
                        logger.info("[ReAct] 流式发送完成，总计 {} 字符", fullResponse.length());

                        // 保存 AI 回复
                        messageService.saveAIMessage(conversationId, answer, username);
                        refreshCacheAsync(conversationId, username);
                        sink.complete();
                    } catch (Exception e) {
                        logger.error("[ReAct] 执行失败: {}", e.getMessage(), e);
                        sink.error(e);
                    }
                }, "react-agent-stream").start();
            });
        } else {
            logger.info("使用普通流式模式");
            // 普通流式对话（使用直接对话prompt，不使用ReAct格式）
            List<org.springframework.ai.chat.messages.Message> allMessages = new ArrayList<>();
            allMessages.add(new SystemMessage(directChatPrompt));
            allMessages.addAll(historyMsgs);
            allMessages.add(new UserMessage(userInput));

            Prompt prompt = new Prompt(allMessages);
            logger.info("发送请求到 AI 模型（Direct模式），消息数量: {}", allMessages.size());

            // 使用AtomicReference保证线程安全的累积
            java.util.concurrent.atomic.AtomicReference<StringBuilder> atomicResponse =
                    new java.util.concurrent.atomic.AtomicReference<>(new StringBuilder());

            return chatModel.stream(prompt)
                    .map(response -> {
                        String text = null;

                        // 详细日志：response结构
                        logger.info("========== [Debug Response] ==========");
                        logger.info("response是否为null: {}", response == null);
                        if (response != null) {
                            logger.info("response.getResult(): {}", response.getResult());
                            if (response.getResult() != null) {
                                logger.info("response.getResult().getOutput(): {}", response.getResult().getOutput());
                                if (response.getResult().getOutput() != null) {
                                    text = response.getResult().getOutput().getText();
                                    logger.info("getText()返回: {} (长度: {})",
                                            text == null ? "null" : "\"" + text + "\"",
                                            text == null ? 0 : text.length());
                                }
                            }
                        }

                        if (text != null && !text.isEmpty()) {
                            atomicResponse.get().append(text);
                            logger.info("收到AI chunk: {} 字符，累计: {} 字符",
                                    text.length(), atomicResponse.get().length());
                        } else {
                            logger.warn("AI返回的text为null或空！这可能导致前端显示错误");
                        }

                        // 流式输出时直接发送原始文本，前端会累积显示
                        String chunk = "{\"content\":\"" + escapeJson(text != null ? text : "") + "\"}";
                        logger.info("发送chunk到前端: {}", chunk);
                        return chunk;
                    })
                    .doOnComplete(() -> {
                        String completeText = atomicResponse.get().toString();
                        logger.info("========== [Stream Chat Complete] ==========");
                        logger.info("总响应长度: {} 字符", completeText.length());
                        logger.info("响应内容预览: {}", truncate(completeText, 200));

                        if (!completeText.isEmpty()) {
                            try {
                                messageService.saveAIMessage(conversationId, completeText, username);
                                logger.info("AI消息已保存");
                            } catch (Exception e) {
                                logger.error("保存AI消息失败: {}", e.getMessage(), e);
                            }
                        } else {
                            logger.warn("警告：AI响应为空！");
                        }
                        refreshCacheAsync(conversationId, username);
                    })
                    .onErrorResume(e -> {
                        logger.error("========== [Stream Chat Error] ==========");
                        logger.error("错误类型: {}", e.getClass().getSimpleName());
                        logger.error("错误消息: {}", e.getMessage());
                        logger.error("错误堆栈: ", e);
                        return Flux.just("{\"code\":500,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                    });
        }
    }

    // ============================================================
    //  Agent 核心逻辑：Think → Plan → Reflect → Reply
    // ============================================================

    /**
     * 执行 ReAct 循环（Think → Plan → Reflect → Reply）
     * - Think: AI 分析问题
     * - Plan: 决定执行搜索还是直接回答
     * - Reflect: 分析搜索结果，判断是否需要更多信息
     * - Reply: 输出最终回答
     */
    private String executeReActLoop(List<org.springframework.ai.chat.messages.Message> historyMsgs,
                                     String userInput) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(agentPrompt));
        messages.addAll(historyMsgs);
        messages.add(new UserMessage(userInput));

        for (int i = 0; i < MAX_REACT_ITERATIONS; i++) {
            // Think + Plan
            logger.info("ReAct 第{}轮 — Think & Plan", i + 1);
            Prompt prompt = new Prompt(messages);
            var response = chatModel.call(prompt);
            String aiText = response.getResult().getOutput().getText();
            logger.info("Agent output: {}", truncate(aiText, 200));

            AgentAction action = parseAction(aiText);

            // Reply
            if (action == null || "answer".equals(action.action)) {
                logger.info("Agent 决定直接回答");
                String result = action != null ? action.input : aiText;
                logger.info("返回回答内容，长度: {}, 前50字符: {}", result.length(), truncate(result, 50));
                return result;
            }

            // Plan: search → 执行搜索
            if ("search".equals(action.action)) {
                logger.info("Agent 执行搜索: {}", action.input);
                String searchResult = webSearchService.search(action.input);
                logger.info("搜索结果: {}", truncate(searchResult, 200));

                // Reflect: 将搜索结果作为 Observation 加入上下文
                messages.add(new AssistantMessage(aiText));
                messages.add(new UserMessage(
                        "Observation: 搜索结果如下：\n" + searchResult +
                        "\n\n请根据以上搜索结果进行反思(Reflect)，判断信息是否充分。" +
                        "如果信息充分，请给出最终回答(action=answer)；" +
                        "如果需要更多信息，请继续搜索(action=search)。"));
            }
        }

        // 超过最大迭代次数，强制回答
        logger.info("达到最大迭代次数 {}，强制回答", MAX_REACT_ITERATIONS);
        messages.add(new UserMessage("你已经搜索了足够多的信息，请直接给出最终回答。输出 action 为 answer。"));
        Prompt finalPrompt = new Prompt(messages);
        var finalResponse = chatModel.call(finalPrompt);
        String finalText = finalResponse.getResult().getOutput().getText();
        AgentAction finalAction = parseAction(finalText);
        return finalAction != null ? finalAction.input : finalText;
    }

    /**
     * 普通对话（无搜索）
     */
    private String executeDirectChat(List<org.springframework.ai.chat.messages.Message> historyMsgs,
                                      String userInput) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(directChatPrompt));
        messages.addAll(historyMsgs);
        messages.add(new UserMessage(userInput));

        Prompt prompt = new Prompt(messages);
        var response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    // ============================================================
    //  图片处理
    // ============================================================

    /**
     * 将图片附件的 base64 嵌入到用户消息中
     */
    private String appendImageDescriptions(String content, List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return content;

        List<Attachment> images = documentAnalysisService.getImageAttachments(attachments);
        if (images.isEmpty()) return content;

        StringBuilder sb = new StringBuilder(content);
        sb.append("\n\n[用户上传了以下图片，请分析图片内容]\n");
        for (Attachment img : images) {
            try {
                String filePath = resolveFilePath(img.url());
                File file = new File(filePath);
                if (file.exists()) {
                    String ext = getExtension(img.filename());
                    String mimeType = ReadFileUtils.getImageMimeType(ext);
                    String base64 = ReadFileUtils.readImageAsBase64(file);
                    sb.append("图片: ").append(img.filename())
                      .append("\n![image](data:").append(mimeType)
                      .append(";base64,").append(base64).append(")\n");
                    logger.info("添加图片: {} ({})", img.filename(), mimeType);
                }
            } catch (Exception e) {
                logger.error("读取图片失败: {}", img.filename(), e);
                sb.append("图片 ").append(img.filename()).append(" 读取失败\n");
            }
        }
        return sb.toString();
    }

    // ============================================================
    //  Agent Action 解析
    // ============================================================

    /**
     * 判断是否需要联网搜索
     */
    private boolean needsSearch(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("搜索") || lower.contains("查一下") || lower.contains("查找")
                || lower.contains("最新") || lower.contains("今天") || lower.contains("天气")
                || lower.contains("新闻") || lower.contains("search") || lower.contains("帮我查")
                || lower.contains("百度") || lower.contains("谷歌") || lower.contains("上网");
    }

    /**
     * 解析 AI 输出的 JSON action
     */
    private AgentAction parseAction(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            String json = text.trim();

            // 去掉 markdown code fence
            if (json.contains("```json")) {
                int start = json.indexOf("```json") + 7;
                int end = json.indexOf("```", start);
                if (end > start) json = json.substring(start, end).trim();
            } else if (json.contains("```")) {
                int start = json.indexOf("```") + 3;
                int end = json.indexOf("```", start);
                if (end > start) json = json.substring(start, end).trim();
            }

            // 提取 JSON 对象
            int braceStart = json.indexOf('{');
            int braceEnd = json.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                json = json.substring(braceStart, braceEnd + 1);
            }

            JSONObject obj = JSONUtil.parseObj(json);
            AgentAction action = new AgentAction();
            action.thought = obj.getStr("thought", "");
            action.action = obj.getStr("action", "answer");
            action.input = obj.getStr("input", "");

            logger.debug("解析Agent action: action={}, input长度={}, thought长度={}",
                    action.action, action.input.length(), action.thought.length());

            return action;
        } catch (Exception e) {
            logger.warn("无法解析 Agent JSON 输出，视为直接回答");
            AgentAction action = new AgentAction();
            action.action = "answer";
            action.input = text;
            return action;
        }
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private List<org.springframework.ai.chat.messages.Message> buildHistoryMessages(List<Message> history) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);
            if (i == history.size() - 1 && "user".equals(msg.getRole())) continue;
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
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

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private String extractUsername(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) return null;
        int colonIndex = conversationId.lastIndexOf(':');
        return colonIndex == -1 ? conversationId : conversationId.substring(0, colonIndex);
    }

    @Async("chatExecutor")
    protected void refreshCacheAsync(String conversationId, String username) {
        try {
            messageService.refreshConversationCache(conversationId, username);
        } catch (Exception e) {
            logger.error("刷新缓存失败: {}", e.getMessage(), e);
        }
    }

    private static class AgentAction {
        String thought;
        String action; // "search" or "answer"
        String input;
    }
}
