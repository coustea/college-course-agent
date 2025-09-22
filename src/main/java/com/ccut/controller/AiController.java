package com.ccut.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ccut.entity.Exam;
import com.ccut.entity.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final BeanOutputConverter<Exam> converter;
    private final String format;

    private static final String PROMPT_TEMPLATE = """
            你是一位经验丰富的出题专家。
            请根据我的要求，围绕主题 "{topic}" 为我生成一份试卷。

            试卷要求如下:
            1. 包含 {judge} 道判断题 (JUDGE)。
            2. 包含 {choice} 道选择题 (CHOICE)。

            对于选择题，必须提供4个选项，并且 'answer' 字段必须是 "A", "B", "C", "D" 中的一个。
            对于判断题，'options' 字段必须为null，'answer' 字段必须是 "true" 或 "false"。
            每道题都必须提供 'analysis' 字段作为题目解析。
            每次生成的试卷和之前的试卷不能重复。

            请务必、严格、只能使用以下JSON格式进行输出。不要在JSON之外添加任何说明或注释。
            {format}
            """;

    public AiController(ChatClient.Builder builder) {
        this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Exam>() {});
        this.format = converter.getFormat();
        this.chatClient = builder
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withModel("qwen-max")
                                .withEnableThinking(true)
                                .withTemperature(1.4)
                                .withEnableSearch(true)
                                .build()
                )
                .build();
    }


    @PostMapping("/generate-exam")
    public Result<Exam> generateExam(@RequestParam String topic,
                                     @RequestParam(defaultValue = "0") Integer judge,
                                     @RequestParam(defaultValue = "1") Integer choice) {
        PromptTemplate template = new PromptTemplate(PROMPT_TEMPLATE);
        Prompt prompt = template.create(Map.of("topic", topic,
                "judge", judge,
                "choice", choice,
                "format", format));
        Exam exam = chatClient.prompt(prompt).call().entity(Exam.class);
        return Result.success(exam);
    }
}


