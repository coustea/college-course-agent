package com.ccut.service;

import com.ccut.dto.IdeologyAnalysisResult;
import com.ccut.dto.IdeologyResourceRequest;
import com.ccut.dto.IdeologyResourceStats;
import com.ccut.entity.IdeologyResource;
import com.ccut.entity.IdeologyResourceRecommendation;
import com.ccut.entity.IdeologyResourceTag;

import java.util.List;

public interface IdeologyResourceService {
    IdeologyResource create(IdeologyResourceRequest request);

    IdeologyResource update(Long resourceId, IdeologyResourceRequest request);

    void delete(Long resourceId);

    IdeologyResource getById(Long resourceId);

    List<IdeologyResource> search(Long courseId, String keyword, String status, Integer limit);

    IdeologyResourceStats getStats(Long courseId);

    IdeologyAnalysisResult analyze(String title, String content);

    List<IdeologyResourceTag> listTags(Long resourceId);

    List<IdeologyResourceRecommendation> getRecommendations(Long studentId, Long courseId, Integer limit);

    List<IdeologyResourceRecommendation> refreshRecommendations(Long studentId, Long courseId, Integer limit);

    void markRecommendationAsClicked(Long recommendationId);
}
