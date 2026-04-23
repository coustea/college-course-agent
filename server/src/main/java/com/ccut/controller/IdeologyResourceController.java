package com.ccut.controller;

import com.ccut.dto.IdeologyAnalysisResult;
import com.ccut.dto.IdeologyResourceRequest;
import com.ccut.dto.IdeologyResourceStats;
import com.ccut.dto.Result;
import com.ccut.entity.IdeologyResource;
import com.ccut.entity.IdeologyResourceRecommendation;
import com.ccut.entity.IdeologyResourceTag;
import com.ccut.service.IdeologyResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ideology/resources")
public class IdeologyResourceController {

    @Autowired
    private IdeologyResourceService ideologyResourceService;

    @PostMapping
    public Result<IdeologyResource> create(@RequestBody IdeologyResourceRequest request) {
        return Result.success(ideologyResourceService.create(request));
    }

    @PutMapping("/{resourceId:\\d+}")
    public Result<IdeologyResource> update(@PathVariable Long resourceId,
                                           @RequestBody IdeologyResourceRequest request) {
        return Result.success(ideologyResourceService.update(resourceId, request));
    }

    @DeleteMapping("/{resourceId:\\d+}")
    public Result<String> delete(@PathVariable Long resourceId) {
        ideologyResourceService.delete(resourceId);
        return Result.success("删除成功");
    }

    @GetMapping("/{resourceId:\\d+}")
    public Result<IdeologyResource> detail(@PathVariable Long resourceId) {
        return Result.success(ideologyResourceService.getById(resourceId));
    }

    @GetMapping
    public Result<List<IdeologyResource>> search(@RequestParam(value = "courseId", required = false) Long courseId,
                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(ideologyResourceService.search(courseId, keyword, status, limit));
    }

    @GetMapping("/stats")
    public Result<IdeologyResourceStats> stats(@RequestParam(value = "courseId", required = false) Long courseId) {
        return Result.success(ideologyResourceService.getStats(courseId));
    }

    @PostMapping("/analyze")
    public Result<IdeologyAnalysisResult> analyze(@RequestBody Map<String, String> request) {
        return Result.success(ideologyResourceService.analyze(request.get("title"), request.get("content")));
    }

    @GetMapping("/{resourceId:\\d+}/tags")
    public Result<List<IdeologyResourceTag>> listTags(@PathVariable Long resourceId) {
        return Result.success(ideologyResourceService.listTags(resourceId));
    }

    @GetMapping("/recommendations")
    public Result<List<IdeologyResourceRecommendation>> recommendations(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(ideologyResourceService.getRecommendations(studentId, courseId, limit));
    }

    @PostMapping("/recommendations/refresh")
    public Result<List<IdeologyResourceRecommendation>> refreshRecommendations(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(ideologyResourceService.refreshRecommendations(studentId, courseId, limit));
    }

    @PostMapping("/recommendations/{id}/click")
    public Result<String> markRecommendationAsClicked(@PathVariable Long id) {
        ideologyResourceService.markRecommendationAsClicked(id);
        return Result.success("标记成功");
    }
}
