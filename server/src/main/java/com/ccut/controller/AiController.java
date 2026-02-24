package com.ccut.controller;

import com.ccut.entity.Exam;
import com.ccut.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 生成试卷控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatModel chatModel;
    private final BeanOutputConverter<Exam> converter;
    private final String format;

    private static final String PROMPT_TEMPLATE = """
            你是一位经验丰富的出题专家。
            请根据我的要求，围绕主题 "{topic}" 为我生成一份试卷。

            试卷要求如下:
            1. 包含 {judge} 道判断题 (JUDGE)。
            2. 包含 {choice} 道选择题 (CHOICE)。

            在题目内容设计上，必须涵盖以下关键词：软件工程、应用开发、课程思政。

            对于选择题，必须提供 4 个选项，并且 'answer' 字段必须是 "A", "B", "C", "D" 中的一个。
            对于判断题，'options' 字段必须为 null，'answer' 字段必须是 "true" 或 "false"。
            每道题都必须提供 'analysis' 字段作为题目解析。
            每次生成的试卷和之前的试卷不能重复。

            请务必、严格、只能使用以下 JSON 格式进行输出。不要在 JSON 之外添加任何说明或注释。
            {format}
            """;


    public AiController(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Exam>() {});
        this.format = converter.getFormat();
    }


    @PostMapping("/generate-exam")
    public Result<Exam> generateExam(@RequestParam String topic,
                                     @RequestParam(defaultValue = "0") Integer judge,
                                     @RequestParam(defaultValue = "1") Integer choice) {
        log.debug("收到生成 AI 试卷请求：URI=/api/ai/generate-exam, 参数：topic={}, judge={}, choice={}", topic, judge, choice);
        try {
            log.info("执行生成 AI 试卷业务：topic={}, judge={}, choice={}", topic, judge, choice);
            long startTime = System.currentTimeMillis();
            
            PromptTemplate template = new PromptTemplate(PROMPT_TEMPLATE);
            Prompt prompt = template.create(Map.of("topic", topic,
                    "judge", judge,
                    "choice", choice,
                    "format", format));
            var response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getText();
            Exam exam = converter.convert(content);
            
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("生成 AI 试卷成功：topic={}, judge={}, choice={}, 耗时={} ms", topic, judge, choice, costTime);
            return Result.success(exam);
        } catch (Exception e) {
            log.error("生成 AI 试卷失败：topic={}, judge={}, choice={}, 错误：{}", topic, judge, choice, e.getMessage(), e);
            return Result.error(500, "生成试卷失败：" + e.getMessage());
        }
    }
}
