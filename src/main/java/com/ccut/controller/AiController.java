package com.ccut.controller;

import com.ccut.entity.Exam;
import com.ccut.entity.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
            1. 包含 3 道选择题 (CHOICE)。
            2. 包含 2 道判断题 (JUDGE)。

            对于选择题，必须提供4个选项，并且 'answer' 字段必须是 "A", "B", "C", "D" 中的一个。
            对于判断题，'options' 字段必须为null，'answer' 字段必须是 "true" 或 "false"。
            每道题都必须提供 'analysis' 字段作为题目解析。

            请务必、严格、只能使用以下JSON格式进行输出。不要在JSON之外添加任何说明或注释。
            {format}
            """;

    public AiController(ChatClient.Builder builder) {
        this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Exam>() {});
        this.format = converter.getFormat();
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam(value = "query", defaultValue = "以影子为作者，写一篇200字左右的有关人工智能诗篇") String query,
                             HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt(query).stream().content();
    }

    @GetMapping("/generate-exam")
    public Result<Exam> generateExam(@RequestParam(defaultValue = "Spring Boot") String topic) {
        PromptTemplate template = new PromptTemplate(PROMPT_TEMPLATE);
        Prompt prompt = template.create(Map.of("topic", topic, "format", format));
        Exam exam = chatClient.prompt(prompt).call().entity(Exam.class);
        return Result.success(exam);
    }
}


