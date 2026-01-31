package com.ccut.service.Impl;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.ccut.dto.ChatRequest;
import com.ccut.dto.ChatResponse;
import com.ccut.entity.Message;
import com.ccut.service.ChatService;
import com.ccut.service.MessageService;
import com.ccut.tools.SearchRequest;
import com.ccut.tools.SearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务实现
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final int MAX_HISTORY_MESSAGES = 20; // 最多加载历史消息数量（作为上下文）

    private final DashScopeChatModel chatModel; // 阿里云DashScope AI模型
    private final ReactAgent reactAgent; // React Agent（支持工具调用的AI代理）

    @Autowired
    private MessageService messageService;

    @Autowired
    public ChatServiceImpl(@Value("${spring.ai.dashscope.api-key}") String apiKey) {
        // 配置联网搜索工具
        ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchTool())
                .inputType(SearchRequest.class)
                .description("联网搜索工具，用于获取实时信息、最新新闻、天气查询、股票行情等需要联网的数据。优先返回直接答案，如无可直接答案则返回相关搜索结果摘要。")
                .build();

        // 配置DashScope AI模型（使用通义千问qwen-max）
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder()
                        .apiKey(apiKey)
                        .build())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .model("qwen-max") // 使用通义千问max模型
                                .enableSearch(true) // 启用联网搜索
                                .enableThinking(true) // 启用思考模式
                                .temperature(0.7) // 温度参数（控制随机性）
                                .topP(0.9) // 采样参数
                                .build()
                )
                .build();

        // 配置ReactAgent（支持工具调用的AI代理）
        this.reactAgent = ReactAgent.builder()
                .systemPrompt("你是一位AI聊天助手，名字叫\"学小微\"。" +
                        "当用户询问你问题时，你先判断自己能否直接回答。" +
                        "如果你会，就直接回答；如果你不会或者需要实时信息、最新数据，就调用搜索工具来获取信息后再回答。" +
                        "请用友好、专业的方式为用户提供帮助。")
                .name("chatAgent")
                .model(this.chatModel)
                .tools(searchTool) // 注入搜索工具
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.conversationId();
        String userMessageContent = request.message();

        logger.info("Processing chat request: conversationId={}", conversationId);

        // 步骤1: 保存用户消息到MySQL（持久化）
        Message userMessage = messageService.saveUserMessage(conversationId, userMessageContent);
        logger.debug("Saved user message: id={}, conversationId={}", userMessage.getId(), conversationId);

        // 步骤2: 加载会话历史（优先从Redis缓存读取，未命中则从MySQL加载）
        List<Message> historyMessages = messageService.loadConversationHistory(conversationId);
        logger.debug("Loaded {} history messages for conversation: {}", historyMessages.size(), conversationId);

        // 步骤3: 构建AI上下文消息列表（历史消息 + 当前用户消息）
        // 注意：排除最后一条（刚刚保存的用户消息），避免重复
        List<org.springframework.ai.chat.messages.Message> allMessages = new ArrayList<>();

        // 添加历史消息（最多20条，作为上下文）
        if (historyMessages.size() > 1) {
            List<Message> contextMessages = historyMessages.subList(0, historyMessages.size() - 1);
            for (Message msg : contextMessages) {
                if ("user".equals(msg.getRole())) {
                    allMessages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    allMessages.add(new AssistantMessage(msg.getContent()));
                }
            }
        }

        // 添加当前用户消息
        UserMessage currentUserMessage = new UserMessage(userMessageContent);
        allMessages.add(currentUserMessage);

        // 步骤4: 调用AI模型（传入完整上下文，ReactAgent会自动判断是否需要联网搜索）
        AssistantMessage aiResponse;
        try {
            aiResponse = reactAgent.call(allMessages);
        } catch (GraphRunnerException e) {
            logger.error("AI call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }

        // 步骤5: 保存AI回复到MySQL（持久化）
        Message aiMessage = messageService.saveAIMessage(conversationId, aiResponse.getText());
        logger.debug("Saved AI message: id={}, conversationId={}", aiMessage.getId(), conversationId);

        // 步骤6: 异步刷新Redis缓存（重新加载完整对话，不阻塞响应）
        refreshCacheAsync(conversationId);

        // 步骤7: 构建响应对象
        ChatResponse response = new ChatResponse();
        response.setUserMessage(userMessage);
        response.setAiMessage(aiMessage);
        response.setConversationId(conversationId);

        return response;
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) throws GraphRunnerException {
        String conversationId = request.conversationId();
        String userMessageContent = request.message();

        logger.info("Processing stream chat request: conversationId={}", conversationId);

        // 步骤1: 保存用户消息到MySQL（持久化）
        Message userMessage = messageService.saveUserMessage(conversationId, userMessageContent);

        // 步骤2: 调用AI模型流式接口（返回响应式流Flux）
        UserMessage currentUserMessage = new UserMessage(userMessageContent);

        return reactAgent.stream(currentUserMessage)
                .filter(output -> {
                    // 过滤：只返回模型推理的流式输出（排除工具调用等中间状态）
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        OutputType type = streamingOutput.getOutputType();
                        return type == OutputType.AGENT_MODEL_STREAMING
                                || type == OutputType.AGENT_MODEL_FINISHED;
                    }
                    return false;
                })
                .map(output -> {
                    // 提取文本内容（将输出对象转为JSON字符串）
                    StreamingOutput<?> streamingOutput = (StreamingOutput<?>) output;
                    return streamingOutput.message().getText();
                })
                .doOnComplete(() -> {
                    // 流式完成后：异步刷新缓存
                    // 注意：由于是流式输出，AI消息的保存需要在接收端处理
                    logger.debug("Stream completed for conversation: {}", conversationId);
                    refreshCacheAsync(conversationId);
                })
                .onErrorResume(GraphRunnerException.class, e -> {
                    logger.error("Stream chat failed: {}", e.getMessage(), e);
                    return Flux.empty();
                });
    }

    /**
     * 异步刷新缓存
     */
    @Async("chatExecutor")
    protected void refreshCacheAsync(String conversationId) {
        try {
            messageService.refreshConversationCache(conversationId);
            logger.debug("Cache refresh initiated for conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Failed to refresh cache for conversation: {}, error: {}",
                    conversationId, e.getMessage(), e);
        }
    }
}
