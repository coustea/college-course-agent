package com.ccut.service.Impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatAgentServiceImpl implements ChatAgentService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAgentServiceImpl.class);
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_REACT_ITERATIONS = 5;

    // 🔴 引入强大的 Jackson ObjectMapper 完美解决 record 类的序列化问题
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String agentPrompt;
    private final String directChatPrompt;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("chatModel")
    private ChatModel chatModel;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("reasoningChatModel")
    private ChatModel reasoningChatModel;

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
        } catch (IOException e) {
            reactPrompt = "你是一个智能教学助手\"学小微\"。输出JSON：{\"thought\":\"...\",\"action\":\"answer\",\"input\":\"...\"}";
        }
        this.agentPrompt = reactPrompt;

        try {
            directPrompt = directPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            directPrompt = "你是一位智能教学助手，名字叫\"学小微\"。请直接、友好地回答用户的问题，使用 Markdown 格式。";
        }
        this.directChatPrompt = directPrompt;
    }

    private String buildAIPrompt(String originalInput, List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return originalInput != null ? originalInput : "请分析附件";
        }

        StringBuilder aiPromptBuilder = new StringBuilder();
        boolean hasContent = false;

        String docContent = documentAnalysisService.analyzeAllAttachments(attachments);
        if (docContent != null && !docContent.isEmpty()) {
            aiPromptBuilder.append("<文档上下文>\n").append(docContent).append("\n</文档上下文>\n\n");
            hasContent = true;
        }

        String imageContext = buildImageDescriptions(attachments);
        if (imageContext != null && !imageContext.isEmpty()) {
            aiPromptBuilder.append("<图片上下文>\n").append(imageContext).append("\n</图片上下文>\n\n");
            hasContent = true;
        }

        if (!hasContent) {
            return originalInput != null ? originalInput : "请分析附件";
        }

        String query = (originalInput != null && !originalInput.trim().isEmpty()) ? originalInput : "请帮我分析上述提供的文件和图片内容，提取核心信息并进行总结。";
        aiPromptBuilder.append("<用户提问>\n").append(query).append("\n</用户提问>\n\n");
        aiPromptBuilder.append("【系统规则】请严格基于上述<文档上下文>和<图片上下文>的内容回答用户的提问。如果资料中找不到答案，请说明。切勿在回答中大段重复照抄上下文的原文。");

        return aiPromptBuilder.toString();
    }

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

    @Override
    public ChatResponse chat(ChatRequest request, String username) {
        String conversationId = request.conversationId();
        String originalInput = request.message();

        logger.info("========== [Chat Start] ==========");

        String aiPrompt = buildAIPrompt(originalInput, request.attachments());
        boolean hasAttachments = request.attachments() != null && !request.attachments().isEmpty();

        String filesJson = null;
        if (hasAttachments) {
            try {
                // 🔴 修复：使用 Jackson 将 Record 正确序列化为 JSON 字符串
                filesJson = objectMapper.writeValueAsString(request.attachments());
            } catch (Exception e) {
                logger.error("附件JSON序列化失败: {}", e.getMessage());
            }
        }
        messageService.saveUserMessage(conversationId, originalInput, username, filesJson);

        List<Message> history = messageService.loadConversationHistory(conversationId, username);
        List<org.springframework.ai.chat.messages.Message> historyMsgs = buildHistoryMessages(history);

        String aiText;
        if (needsSearch(aiPrompt)) {
            aiText = executeReActLoop(historyMsgs, aiPrompt);
        } else {
            aiText = executeDirectChat(historyMsgs, aiPrompt);
        }

        Message aiMessage = messageService.saveAIMessage(conversationId, aiText, username);
        refreshCacheAsync(conversationId, username);

        ChatResponse response = new ChatResponse();
        response.setAiMessage(aiMessage);
        response.setConversationId(conversationId);
        return response;
    }

    @Override
    public Flux<String> chatStream(ChatRequest request, String username) {
        String conversationId = request.conversationId();
        String originalInput = request.message();
        boolean hasAttachments = request.attachments() != null && !request.attachments().isEmpty();

        logger.info("========== [Stream Chat Start] ==========");

        String aiPrompt = buildAIPrompt(originalInput, request.attachments());

        try {
            String filesJson = null;
            if (hasAttachments) {
                // 🔴 修复：使用 Jackson 将 Record 正确序列化为 JSON 字符串
                filesJson = objectMapper.writeValueAsString(request.attachments());
            }
            messageService.saveUserMessage(conversationId, originalInput, username, filesJson);
        } catch (Exception e) {
            logger.error("保存用户消息失败: {}", e.getMessage(), e);
            return Flux.just("{\"code\":500,\"message\":\"保存消息失败\"}");
        }

        List<Message> history;
        try {
            history = messageService.loadConversationHistory(conversationId, username);
        } catch (Exception e) {
            history = List.of();
        }
        List<org.springframework.ai.chat.messages.Message> historyMsgs = buildHistoryMessages(history);

        StringBuilder fullResponse = new StringBuilder();

        if (needsSearch(aiPrompt)) {
            final String finalInput = aiPrompt;
            return Flux.create(sink -> {
                new Thread(() -> {
                    try {
                        String answer = executeReActLoop(historyMsgs, finalInput);
                        int chunkSize = 4;
                        for (int i = 0; i < answer.length(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, answer.length());
                            String chunk = answer.substring(i, end);
                            fullResponse.append(chunk);
                            sink.next("{\"content\":\"" + escapeJson(chunk) + "\"}");
                            Thread.sleep(15);
                        }
                        messageService.saveAIMessage(conversationId, answer, username);
                        refreshCacheAsync(conversationId, username);
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }, "react-agent-stream").start();
            });
        } else {
            List<org.springframework.ai.chat.messages.Message> allMessages = new ArrayList<>();
            allMessages.add(new SystemMessage(directChatPrompt));
            allMessages.addAll(historyMsgs);
            allMessages.add(new UserMessage(aiPrompt));

            Prompt prompt = new Prompt(allMessages);
            AtomicReference<StringBuilder> atomicResponse = new AtomicReference<>(new StringBuilder());

            return chatModel.stream(prompt)
                    .map(response -> {
                        String text = null;
                        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                            text = response.getResult().getOutput().getText();
                        }
                        if (text != null && !text.isEmpty()) {
                            atomicResponse.get().append(text);
                        }
                        return "{\"content\":\"" + escapeJson(text != null ? text : "") + "\"}";
                    })
                    .doOnComplete(() -> {
                        String completeText = atomicResponse.get().toString();
                        if (!completeText.isEmpty()) {
                            try {
                                messageService.saveAIMessage(conversationId, completeText, username);
                            } catch (Exception e) {
                                logger.error("保存AI消息失败: {}", e.getMessage(), e);
                            }
                        }
                        refreshCacheAsync(conversationId, username);
                    });
        }
    }

    private String executeReActLoop(List<org.springframework.ai.chat.messages.Message> historyMsgs, String aiPrompt) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(agentPrompt));
        messages.addAll(historyMsgs);
        messages.add(new UserMessage(aiPrompt));

        boolean hasAttachment = aiPrompt != null && (
                aiPrompt.contains("<文档上下文>") || aiPrompt.contains("<图片上下文>"));

        if (hasAttachment) {
            logger.info("检测到用户上传了附件，直接让AI分析附件内容");
            messages.add(new UserMessage(
                    "\n\n重要提示：用户提供了文档或图片资料，请优先严格基于上方提供的<文档上下文>和<图片上下文>回答，不要盲目联网搜索。" +
                            "\n如果上下文内容足够解答，请使用 action=answer。"));
        } else {
            String firstSearchQuery = optimizeSearchQuery(aiPrompt);
            String firstSearchResult = webSearchService.search(firstSearchQuery);
            messages.add(new AssistantMessage("{\"thought\":\"先使用用户原始问题搜索\",\"action\":\"search\",\"input\":\"" + escapeJson(firstSearchQuery) + "\"}"));
            messages.add(new UserMessage(
                    "Observation: 使用你原始问题的搜索结果如下：\n" + firstSearchResult +
                            "\n\n请分析这些搜索结果和用户的原始问题，决定(answer/search/clarify)"));
        }

        for (int i = 1; i < MAX_REACT_ITERATIONS; i++) {
            Prompt prompt = new Prompt(messages);
            var response = reasoningChatModel.call(prompt);
            String aiText = response.getResult().getOutput().getText();
            AgentAction action = parseAction(aiText);

            if (action == null || "clarify".equals(action.action)) {
                return action != null ? action.input : aiText;
            }

            if ("answer".equals(action.action)) {
                return extractCleanAnswer(action.input);
            }

            if ("search".equals(action.action)) {
                String searchQuery = optimizeSearchQuery(action.input);
                String searchResult = webSearchService.search(searchQuery);
                messages.add(new AssistantMessage(aiText));
                messages.add(new UserMessage(
                        "Observation: 搜索结果如下：\n" + searchResult +
                                "\n\n判断信息是否充分。充分请answer；不足请继续search。"));
            }
        }

        messages.add(new UserMessage("你已经搜索了足够多的信息，请直接给出最终回答。输出 action 为 answer。"));
        var finalResponse = reasoningChatModel.call(new Prompt(messages));
        AgentAction finalAction = parseAction(finalResponse.getResult().getOutput().getText());
        return extractCleanAnswer(finalAction != null ? finalAction.input : finalResponse.getResult().getOutput().getText());
    }

    private String executeDirectChat(List<org.springframework.ai.chat.messages.Message> historyMsgs, String aiPrompt) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(directChatPrompt));
        messages.addAll(historyMsgs);
        messages.add(new UserMessage(aiPrompt));
        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    private boolean needsSearch(String message) {
        return true;
    }

    private AgentAction parseAction(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            String json = text.trim();
            if (json.contains("```json")) {
                int start = json.indexOf("```json") + 7;
                int end = json.indexOf("```", start);
                if (end > start) json = json.substring(start, end).trim();
            } else if (json.contains("```")) {
                int start = json.indexOf("```") + 3;
                int end = json.indexOf("```", start);
                if (end > start) json = json.substring(start, end).trim();
            }
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
            return action;
        } catch (Exception e) {
            AgentAction action = new AgentAction();
            action.action = "answer";
            action.input = text;
            return action;
        }
    }

    private List<org.springframework.ai.chat.messages.Message> buildHistoryMessages(List<Message> history) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);

        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);

            if (i == history.size() - 1 && "user".equals(msg.getRole())) continue;

            if ("user".equals(msg.getRole())) {
                String historyContent = msg.getContent();

                if (msg.getFiles() != null && !msg.getFiles().trim().isEmpty()
                        && !msg.getFiles().equals("[]") && !msg.getFiles().equals("null")) {
                    try {
                        // 🔴 修复：同样使用 Jackson 来反序列化，支持 record 还原
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
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String extractCleanAnswer(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.trim().isEmpty()) return rawAnswer;
        int answerMarkIndex = rawAnswer.indexOf("[答案]:");
        if (answerMarkIndex != -1) {
            return rawAnswer.substring(answerMarkIndex + "[答案]:".length()).trim();
        }
        return rawAnswer;
    }

    private String optimizeSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return query;
        String lowerQuery = query.toLowerCase();
        String[] countryKeywords = {"中国", "国内", "美国", "欧洲", "日本", "韩国", "俄罗斯", "英国", "法国", "德国", "国际", "全球", "世界"};
        for (String keyword : countryKeywords) {
            if (lowerQuery.contains(keyword)) return query;
        }
        String[] politicalKeywords = {"时事", "新闻", "政策", "政治", "经济", "社会", "政府", "法律", "发布会", "最新"};
        for (String keyword : politicalKeywords) {
            if (lowerQuery.contains(keyword)) return "中国 " + query;
        }
        return query;
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
        String action;
        String input;
    }
}