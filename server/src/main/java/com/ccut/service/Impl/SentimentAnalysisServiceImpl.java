package com.ccut.service.Impl;

import com.ccut.dto.SentimentAnalysisResult;
import com.ccut.dto.SentimentDistribution;
import com.ccut.entity.Message;
import com.ccut.entity.SentimentRecord;
import com.ccut.mapper.MessageMapper;
import com.ccut.mapper.SentimentRecordMapper;
import com.ccut.service.SentimentAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 情感分析服务实现类
 * 使用AI进行情感分析
 */
@Slf4j
@Service
public class SentimentAnalysisServiceImpl implements SentimentAnalysisService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SENTIMENT_PROMPT = """
            你是情感分析专家。请分析以下文本的情感倾向。
            输出必须是 JSON 格式，不要包含 Markdown 代码块：
            {
              "sentimentType": "positive/neutral/negative",
              "positiveScore": 0.0-1.0,
              "negativeScore": 0.0-1.0,
              "neutralScore": 0.0-1.0,
              "intensity": 0.0-1.0,
              "positiveWords": ["积极词1", "积极词2"],
              "negativeWords": ["消极词1"],
              "analysis": "简短分析说明"
            }

            文本：{text}
            """;

    private static final String IDEOLOGY_SENTIMENT_PROMPT = """
            你是高校课程思政教育专家。请分析以下文本中学生对思政主题的情感态度。
            思政主题：{theme}

            输出必须是 JSON 格式：
            {
              "sentimentType": "positive/neutral/negative",
              "positiveScore": 0.0-1.0,
              "negativeScore": 0.0-1.0,
              "neutralScore": 0.0-1.0,
              "intensity": 0.0-1.0,
              "ideologyRelated": true/false,
              "analysis": "学生对该思政主题的态度分析"
            }

            文本：{text}
            """;

    @Autowired
    private SentimentRecordMapper sentimentRecordMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    @Qualifier("chatModel")
    private ChatModel chatModel;

    @Override
    public SentimentAnalysisResult analyzeSentiment(String text) {
        if (text == null || text.isBlank()) {
            return SentimentAnalysisResult.neutral();
        }

        try {
            PromptTemplate template = new PromptTemplate(SENTIMENT_PROMPT);
            Prompt prompt = template.create(Map.of("text", text));
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            return OBJECT_MAPPER.readValue(stripJson(response), SentimentAnalysisResult.class);
        } catch (Exception e) {
            log.warn("AI情感分析失败，使用规则分析: {}", e.getMessage());
            return ruleBasedAnalysis(text);
        }
    }

    @Override
    public List<SentimentRecord> getStudentSentimentTrend(Long studentId, Long courseId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        return sentimentRecordMapper.findByStudentAndCourseAfter(studentId, courseId, startTime);
    }

    @Override
    public void recordSentiment(Long studentId, Long courseId, String sourceType,
                                 String sourceContent, Long sourceId) {
        SentimentAnalysisResult result = analyzeSentiment(sourceContent);

        SentimentRecord record = new SentimentRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setSourceType(sourceType);
        record.setSourceContent(sourceContent.length() > 200 ? sourceContent.substring(0, 200) : sourceContent);
        record.setSourceId(sourceId);
        record.setSentimentType(result.getSentimentType());
        record.setPositiveScore(result.getPositiveScore());
        record.setNegativeScore(result.getNegativeScore());
        record.setNeutralScore(result.getNeutralScore());
        record.setAnalyzedAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());

        sentimentRecordMapper.insert(record);
    }

    @Override
    public SentimentDistribution getCourseSentimentDistribution(Long courseId) {
        List<SentimentRecord> records = sentimentRecordMapper.findByCourseId(courseId, 1000);

        SentimentDistribution distribution = new SentimentDistribution();
        if (records.isEmpty()) {
            distribution.setPositiveRatio(0.0);
            distribution.setNegativeRatio(0.0);
            distribution.setNeutralRatio(0.0);
            distribution.setTotalRecords(0);
            return distribution;
        }

        long positive = records.stream().filter(r -> "positive".equals(r.getSentimentType())).count();
        long negative = records.stream().filter(r -> "negative".equals(r.getSentimentType())).count();
        long neutral = records.stream().filter(r -> "neutral".equals(r.getSentimentType())).count();
        int total = records.size();

        distribution.setPositiveRatio((double) positive / total);
        distribution.setNegativeRatio((double) negative / total);
        distribution.setNeutralRatio((double) neutral / total);
        distribution.setTotalRecords(total);

        return distribution;
    }

    @Override
    public SentimentAnalysisResult analyzeIdeologySentiment(String text, String ideologyTheme) {
        if (text == null || text.isBlank()) {
            SentimentAnalysisResult result = SentimentAnalysisResult.neutral();
            result.setIdeologyRelated(false);
            return result;
        }

        try {
            PromptTemplate template = new PromptTemplate(IDEOLOGY_SENTIMENT_PROMPT);
            Prompt prompt = template.create(Map.of(
                    "text", text,
                    "theme", ideologyTheme != null ? ideologyTheme : "课程思政"
            ));
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            SentimentAnalysisResult result = OBJECT_MAPPER.readValue(stripJson(response), SentimentAnalysisResult.class);
            result.setIdeologyRelated(true);
            result.setIdeologyTheme(ideologyTheme);
            return result;
        } catch (Exception e) {
            log.warn("思政情感分析失败: {}", e.getMessage());
            return ruleBasedAnalysis(text);
        }
    }

    private SentimentAnalysisResult ruleBasedAnalysis(String text) {
        SentimentAnalysisResult result = new SentimentAnalysisResult();

        // 简单的规则分析
        String lowerText = text.toLowerCase();

        List<String> positiveWords = Arrays.asList("好", "喜欢", "棒", "优秀", "感谢", "收获", "有帮助", "很好", "不错");
        List<String> negativeWords = Arrays.asList("不好", "讨厌", "无聊", "困难", "问题", "差", "失望");

        int positiveCount = 0;
        int negativeCount = 0;
        List<String> foundPositive = new ArrayList<>();
        List<String> foundNegative = new ArrayList<>();

        for (String word : positiveWords) {
            if (lowerText.contains(word)) {
                positiveCount++;
                foundPositive.add(word);
            }
        }

        for (String word : negativeWords) {
            if (lowerText.contains(word)) {
                negativeCount++;
                foundNegative.add(word);
            }
        }

        double positiveScore = positiveCount * 0.2;
        double negativeScore = negativeCount * 0.2;

        if (positiveCount > negativeCount) {
            result.setSentimentType("positive");
            result.setPositiveScore(Math.min(0.5 + positiveScore * 0.1, 1.0));
            result.setNegativeScore(negativeScore * 0.1);
            result.setNeutralScore(1 - result.getPositiveScore() - result.getNegativeScore());
        } else if (negativeCount > positiveCount) {
            result.setSentimentType("negative");
            result.setNegativeScore(Math.min(0.5 + negativeScore * 0.1, 1.0));
            result.setPositiveScore(positiveScore * 0.1);
            result.setNeutralScore(1 - result.getPositiveScore() - result.getNegativeScore());
        } else {
            result.setSentimentType("neutral");
            result.setNeutralScore(0.6);
            result.setPositiveScore(0.2);
            result.setNegativeScore(0.2);
        }

        result.setPositiveWords(foundPositive);
        result.setNegativeWords(foundNegative);
        result.setIntensity(0.5);

        return result;
    }

    private String stripJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }
}
