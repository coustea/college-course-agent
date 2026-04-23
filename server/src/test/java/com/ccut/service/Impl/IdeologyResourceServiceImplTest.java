package com.ccut.service.Impl;

import com.ccut.dto.IdeologyResourceStats;
import com.ccut.entity.IdeologyResource;
import com.ccut.mapper.IdeologyResourceMapper;
import com.ccut.mapper.IdeologyResourceRecommendationMapper;
import com.ccut.mapper.IdeologyResourceTagMapper;
import com.ccut.mapper.LearningProgressMapper;
import com.ccut.mapper.WrongQuestionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdeologyResourceServiceImplTest {

    @Test
    void getStatsAggregatesCountsForDashboard() {
        IdeologyResourceMapper resourceMapper = mock(IdeologyResourceMapper.class);
        IdeologyResourceTagMapper tagMapper = mock(IdeologyResourceTagMapper.class);
        IdeologyResourceRecommendationMapper recommendationMapper = mock(IdeologyResourceRecommendationMapper.class);
        LearningProgressMapper learningProgressMapper = mock(LearningProgressMapper.class);
        WrongQuestionMapper wrongQuestionMapper = mock(WrongQuestionMapper.class);
        ChatModel chatModel = mock(ChatModel.class);

        when(resourceMapper.countAll(null)).thenReturn(12L);
        when(resourceMapper.countByStatus("published", null)).thenReturn(9L);
        when(resourceMapper.countByStatus("draft", null)).thenReturn(3L);
        when(resourceMapper.countPendingSource(null)).thenReturn(2L);
        when(resourceMapper.countByResourceType(null)).thenReturn(List.of(
                Map.of("resourceType", "document", "count", 7L),
                Map.of("resourceType", "video", "count", 5L)
        ));
        when(resourceMapper.countByValueTheme(null)).thenReturn(List.of(
                Map.of("valueTheme", "爱国主义", "count", 6L),
                Map.of("valueTheme", "创新精神", "count", 6L)
        ));
        when(resourceMapper.countByApplicableScene(null)).thenReturn(List.of(
                Map.of("applicableScene", "课堂导入", "count", 8L),
                Map.of("applicableScene", "案例分析", "count", 4L)
        ));
        when(resourceMapper.countDistinctCourseCoverage(null)).thenReturn(4L);

        IdeologyResourceServiceImpl service = new IdeologyResourceServiceImpl(
                resourceMapper,
                tagMapper,
                recommendationMapper,
                learningProgressMapper,
                wrongQuestionMapper,
                chatModel
        );

        IdeologyResourceStats stats = service.getStats(null);

        assertEquals(100L, stats.getTargetCount());
        assertEquals(12L, stats.getTotalCount());
        assertEquals(9L, stats.getPublishedCount());
        assertEquals(3L, stats.getDraftCount());
        assertEquals(2L, stats.getPendingSourceCount());
        assertEquals(4L, stats.getCourseCoverageCount());
        assertEquals(12.0, stats.getCompletionRate());
        assertEquals(75.0, stats.getPublishedRate());
        assertEquals(7L, stats.getResourceTypeCounts().get("document"));
        assertEquals(6L, stats.getValueThemeCounts().get("爱国主义"));
        assertEquals(8L, stats.getApplicableSceneCounts().get("课堂导入"));
    }
}
