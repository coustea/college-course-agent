package com.ccut.service.Impl;

import com.ccut.dto.IdeologyAnalysisResult;
import com.ccut.dto.IdeologyResourceRequest;
import com.ccut.dto.IdeologyResourceStats;
import com.ccut.entity.IdeologyResource;
import com.ccut.entity.IdeologyResourceRecommendation;
import com.ccut.entity.IdeologyResourceTag;
import com.ccut.entity.LearningProgress;
import com.ccut.entity.WrongQuestion;
import com.ccut.mapper.IdeologyResourceMapper;
import com.ccut.mapper.IdeologyResourceRecommendationMapper;
import com.ccut.mapper.IdeologyResourceTagMapper;
import com.ccut.mapper.LearningProgressMapper;
import com.ccut.mapper.WrongQuestionMapper;
import com.ccut.service.IdeologyResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IdeologyResourceServiceImpl implements IdeologyResourceService {

    private static final int DEFAULT_LIMIT = 20;
    private static final long PROJECT_RESOURCE_TARGET = 100L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ANALYZE_PROMPT = """
            你是高校课程思政资源分析专家。请分析以下教学资源，抽取课程思政结构化信息。
            输出必须是 JSON，不要包含 Markdown 代码块。
            JSON 字段：
            {
              "summary": "100字以内摘要",
              "valueTheme": "价值主题",
              "applicableScene": "适用教学场景",
              "keywords": ["关键词1", "关键词2"],
              "tags": ["标签1", "标签2"],
              "difficulty": "easy|medium|hard"
            }
            标题：{title}
            内容：{content}
            """;

    private final IdeologyResourceMapper resourceMapper;
    private final IdeologyResourceTagMapper tagMapper;
    private final IdeologyResourceRecommendationMapper recommendationMapper;
    private final LearningProgressMapper learningProgressMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ChatModel chatModel;

    public IdeologyResourceServiceImpl(IdeologyResourceMapper resourceMapper,
                                       IdeologyResourceTagMapper tagMapper,
                                       IdeologyResourceRecommendationMapper recommendationMapper,
                                       LearningProgressMapper learningProgressMapper,
                                       WrongQuestionMapper wrongQuestionMapper,
                                       @Qualifier("chatModel") ChatModel chatModel) {
        this.resourceMapper = resourceMapper;
        this.tagMapper = tagMapper;
        this.recommendationMapper = recommendationMapper;
        this.learningProgressMapper = learningProgressMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.chatModel = chatModel;
    }

    @Override
    @Transactional
    public IdeologyResource create(IdeologyResourceRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("资源标题不能为空");
        }
        IdeologyResource resource = toEntity(request);
        if (resource.getStatus() == null || resource.getStatus().isBlank()) {
            resource.setStatus("draft");
        }
        if (resource.getResourceType() == null || resource.getResourceType().isBlank()) {
            resource.setResourceType("document");
        }

        IdeologyAnalysisResult analysis = null;
        if (Boolean.TRUE.equals(request.getAutoAnalyze())) {
            analysis = analyze(request.getTitle(), request.getContentSummary());
            applyAnalysis(resource, analysis);
        }

        resourceMapper.insert(resource);
        attachTags(resource.getResourceId(), collectTags(resource, analysis));
        return resourceMapper.selectById(resource.getResourceId());
    }

    @Override
    @Transactional
    public IdeologyResource update(Long resourceId, IdeologyResourceRequest request) {
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId 不能为空");
        }
        IdeologyResource existing = resourceMapper.selectById(resourceId);
        if (existing == null) {
            throw new RuntimeException("思政资源不存在");
        }

        IdeologyResource patch = toEntity(request);
        patch.setResourceId(resourceId);
        IdeologyAnalysisResult analysis = null;
        if (request != null && Boolean.TRUE.equals(request.getAutoAnalyze())) {
            String title = patch.getTitle() == null ? existing.getTitle() : patch.getTitle();
            String content = patch.getContentSummary() == null ? existing.getContentSummary() : patch.getContentSummary();
            analysis = analyze(title, content);
            applyAnalysis(patch, analysis);
        }

        int updated = resourceMapper.updateById(patch);
        if (updated <= 0) {
            throw new RuntimeException("思政资源更新失败");
        }
        if (analysis != null || (request != null && request.getKeywords() != null)) {
            tagMapper.deleteResourceTags(resourceId);
            IdeologyResource latest = resourceMapper.selectById(resourceId);
            attachTags(resourceId, collectTags(latest, analysis));
        }
        return resourceMapper.selectById(resourceId);
    }

    @Override
    @Transactional
    public void delete(Long resourceId) {
        if (resourceMapper.deleteById(resourceId) <= 0) {
            throw new RuntimeException("思政资源不存在");
        }
    }

    @Override
    public IdeologyResource getById(Long resourceId) {
        return resourceMapper.selectById(resourceId);
    }

    @Override
    public List<IdeologyResource> search(Long courseId, String keyword, String status, Integer limit) {
        return resourceMapper.search(courseId, keyword, status, normalizeLimit(limit));
    }

    @Override
    public IdeologyResourceStats getStats(Long courseId) {
        Long totalCount = safeLong(resourceMapper.countAll(courseId));
        Long publishedCount = safeLong(resourceMapper.countByStatus("published", courseId));
        return new IdeologyResourceStats(
                PROJECT_RESOURCE_TARGET,
                totalCount,
                publishedCount,
                safeLong(resourceMapper.countByStatus("draft", courseId)),
                safeLong(resourceMapper.countPendingSource(courseId)),
                safeLong(resourceMapper.countDistinctCourseCoverage(courseId)),
                percent(totalCount, PROJECT_RESOURCE_TARGET),
                percent(publishedCount, totalCount),
                toCountMap(resourceMapper.countByResourceType(courseId), "resourceType"),
                toCountMap(resourceMapper.countByValueTheme(courseId), "valueTheme"),
                toCountMap(resourceMapper.countByApplicableScene(courseId), "applicableScene")
        );
    }

    @Override
    public IdeologyAnalysisResult analyze(String title, String content) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        try {
            PromptTemplate template = new PromptTemplate(ANALYZE_PROMPT);
            Prompt prompt = template.create(Map.of(
                    "title", safeTitle,
                    "content", safeContent
            ));
            String text = chatModel.call(prompt).getResult().getOutput().getText();
            return OBJECT_MAPPER.readValue(stripJson(text), IdeologyAnalysisResult.class);
        } catch (Exception e) {
            log.warn("AI 思政资源分析失败，使用关键词兜底：title={}, error={}", safeTitle, e.getMessage());
            return fallbackAnalysis(safeTitle, safeContent);
        }
    }

    @Override
    public List<IdeologyResourceTag> listTags(Long resourceId) {
        return tagMapper.listByResourceId(resourceId);
    }

    @Override
    public List<IdeologyResourceRecommendation> getRecommendations(Long studentId, Long courseId, Integer limit) {
        List<IdeologyResourceRecommendation> existing = recommendationMapper.findByStudentId(studentId, normalizeLimit(limit));
        if (!existing.isEmpty()) {
            return existing;
        }
        return refreshRecommendations(studentId, courseId, limit);
    }

    @Override
    @Transactional
    public List<IdeologyResourceRecommendation> refreshRecommendations(Long studentId, Long courseId, Integer limit) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        int safeLimit = normalizeLimit(limit);
        recommendationMapper.deleteByStudentId(studentId);

        Set<String> profileKeywords = buildStudentProfileKeywords(studentId, courseId);
        List<IdeologyResource> candidates = resourceMapper.findPublishedByKeywords(courseId, new ArrayList<>(profileKeywords), safeLimit * 3);
        if (candidates.size() < safeLimit) {
            candidates.addAll(resourceMapper.search(courseId, null, "published", safeLimit * 3));
        }

        List<IdeologyResourceRecommendation> recommendations = candidates.stream()
                .collect(Collectors.toMap(IdeologyResource::getResourceId, resource -> resource, (a, b) -> a))
                .values()
                .stream()
                .map(resource -> scoreResource(studentId, profileKeywords, resource))
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(safeLimit)
                .collect(Collectors.toList());

        for (IdeologyResourceRecommendation recommendation : recommendations) {
            recommendationMapper.insert(recommendation);
        }
        return recommendationMapper.findByStudentId(studentId, safeLimit);
    }

    @Override
    public void markRecommendationAsClicked(Long recommendationId) {
        recommendationMapper.markAsClicked(recommendationId, LocalDateTime.now());
    }

    private IdeologyResource toEntity(IdeologyResourceRequest request) {
        IdeologyResource resource = new IdeologyResource();
        if (request == null) {
            return resource;
        }
        resource.setCourseId(request.getCourseId());
        resource.setChapterId(request.getChapterId());
        resource.setVideoId(request.getVideoId());
        resource.setDocumentId(request.getDocumentId());
        resource.setTitle(request.getTitle());
        resource.setResourceType(request.getResourceType());
        resource.setContentSummary(request.getContentSummary());
        resource.setSourceUrl(request.getSourceUrl());
        resource.setValueTheme(request.getValueTheme());
        resource.setApplicableScene(request.getApplicableScene());
        resource.setKeywords(request.getKeywords());
        resource.setDifficulty(request.getDifficulty());
        resource.setStatus(request.getStatus());
        resource.setCreatedBy(request.getCreatedBy());
        return resource;
    }

    private void applyAnalysis(IdeologyResource resource, IdeologyAnalysisResult analysis) {
        if (analysis == null) {
            return;
        }
        if (isBlank(resource.getContentSummary())) {
            resource.setContentSummary(analysis.getSummary());
        }
        if (isBlank(resource.getValueTheme())) {
            resource.setValueTheme(analysis.getValueTheme());
        }
        if (isBlank(resource.getApplicableScene())) {
            resource.setApplicableScene(analysis.getApplicableScene());
        }
        if (isBlank(resource.getDifficulty())) {
            resource.setDifficulty(analysis.getDifficulty());
        }
        if (isBlank(resource.getKeywords()) && analysis.getKeywords() != null) {
            resource.setKeywords(String.join(",", analysis.getKeywords()));
        }
    }

    private void attachTags(Long resourceId, Set<String> tags) {
        for (String tagName : tags) {
            if (tagName == null || tagName.isBlank()) {
                continue;
            }
            IdeologyResourceTag tag = new IdeologyResourceTag();
            tag.setTagName(tagName.trim());
            tag.setTagType("keyword");
            tagMapper.insertIgnoreDuplicate(tag);
            IdeologyResourceTag saved = tagMapper.selectByName(tag.getTagName());
            if (saved != null && saved.getTagId() != null) {
                tagMapper.attachTag(resourceId, saved.getTagId());
            }
        }
    }

    private Set<String> collectTags(IdeologyResource resource, IdeologyAnalysisResult analysis) {
        Set<String> tags = new LinkedHashSet<>();
        addSplit(tags, resource.getKeywords());
        add(tags, resource.getValueTheme());
        add(tags, resource.getApplicableScene());
        if (analysis != null) {
            if (analysis.getKeywords() != null) {
                tags.addAll(analysis.getKeywords());
            }
            if (analysis.getTags() != null) {
                tags.addAll(analysis.getTags());
            }
        }
        return tags;
    }

    private Set<String> buildStudentProfileKeywords(Long studentId, Long courseId) {
        Set<String> keywords = new LinkedHashSet<>();
        add(keywords, "课程思政");
        if (courseId != null) {
            LearningProgress progress = learningProgressMapper.findOne(studentId, courseId);
            if (progress == null || progress.getCompletionPercentage() == null || progress.getCompletionPercentage() < 60) {
                add(keywords, "学习引导");
                add(keywords, "责任意识");
            } else {
                add(keywords, "拓展实践");
                add(keywords, "家国情怀");
            }
        }
        try {
            List<WrongQuestion> wrongQuestions = wrongQuestionMapper.selectByStudentId(studentId, courseId);
            if (wrongQuestions != null && !wrongQuestions.isEmpty()) {
                add(keywords, "巩固提升");
                add(keywords, "反思表达");
            }
        } catch (Exception e) {
            log.debug("构建学生画像时错题查询失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage());
        }
        return keywords;
    }

    private IdeologyResourceRecommendation scoreResource(Long studentId, Set<String> profileKeywords, IdeologyResource resource) {
        double score = 0.45;
        String haystack = ((resource.getKeywords() == null ? "" : resource.getKeywords()) + ","
                + (resource.getValueTheme() == null ? "" : resource.getValueTheme()) + ","
                + (resource.getApplicableScene() == null ? "" : resource.getApplicableScene()))
                .toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String keyword : profileKeywords) {
            if (keyword != null && haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += 0.15;
                matched.add(keyword);
            }
        }
        if ("easy".equals(resource.getDifficulty())) {
            score += 0.05;
        }
        score = Math.min(1.0, score);

        IdeologyResourceRecommendation recommendation = new IdeologyResourceRecommendation();
        recommendation.setStudentId(studentId);
        recommendation.setResourceId(resource.getResourceId());
        recommendation.setCourseId(resource.getCourseId());
        recommendation.setScore(score);
        recommendation.setRecommendationType(matched.isEmpty() ? "POPULAR" : "TAG_MATCH");
        recommendation.setReason(matched.isEmpty()
                ? "课程思政资源库中的通用推荐"
                : "匹配你的学习画像：" + String.join("、", matched));
        recommendation.setHasClicked(false);
        recommendation.setCreatedAt(LocalDateTime.now());
        return recommendation;
    }

    private IdeologyAnalysisResult fallbackAnalysis(String title, String content) {
        IdeologyAnalysisResult result = new IdeologyAnalysisResult();
        result.setSummary(firstNonBlank(content, title));
        result.setValueTheme(inferTheme(title + " " + content));
        result.setApplicableScene("课程导入与课后拓展");
        result.setKeywords(List.of("课程思政", result.getValueTheme(), "价值引导"));
        result.setTags(result.getKeywords());
        result.setDifficulty("medium");
        return result;
    }

    private String inferTheme(String text) {
        if (text == null) {
            return "价值引导";
        }
        if (text.contains("国家") || text.contains("政策")) {
            return "家国情怀";
        }
        if (text.contains("工程") || text.contains("技术") || text.contains("安全")) {
            return "工程伦理";
        }
        if (text.contains("创新") || text.contains("创业")) {
            return "创新精神";
        }
        return "责任意识";
    }

    private String stripJson(String text) {
        if (text == null) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows, String keyField) {
        Map<String, Long> result = new java.util.LinkedHashMap<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            Object key = row.get(keyField);
            Object count = row.get("count");
            if (key == null || count == null) {
                continue;
            }
            result.put(String.valueOf(key), safeLong(count));
        }
        return result;
    }

    private Long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Double percent(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator <= 0) {
            return 0.0;
        }
        return Math.min(100.0, Math.round(numerator * 10000.0 / denominator) / 100.0);
    }

    private void addSplit(Set<String> values, String text) {
        if (text == null) {
            return;
        }
        for (String item : text.split("[,，;；\\s]+")) {
            add(values, item);
        }
    }

    private void add(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private String firstNonBlank(String first, String second) {
        String value = isBlank(first) ? second : first;
        if (value == null) {
            return "";
        }
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
