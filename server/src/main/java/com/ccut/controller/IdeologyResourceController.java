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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ideology/resources")
public class IdeologyResourceController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Autowired
    private IdeologyResourceService ideologyResourceService;

    @PostMapping
    public Result<IdeologyResource> create(@RequestBody IdeologyResourceRequest request) {
        return Result.success(ideologyResourceService.create(request));
    }

    @PostMapping("/upload")
    public Result<IdeologyResource> createWithFile(
            @RequestParam("title") String title,
            @RequestParam(value = "resourceType", required = false, defaultValue = "document") String resourceType,
            @RequestParam(value = "contentSummary", required = false) String contentSummary,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "valueTheme", required = false) String valueTheme,
            @RequestParam(value = "applicableScene", required = false) String applicableScene,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "difficulty", required = false, defaultValue = "medium") String difficulty,
            @RequestParam(value = "status", required = false, defaultValue = "draft") String status,
            @RequestParam(value = "autoAnalyze", required = false, defaultValue = "false") Boolean autoAnalyze,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            IdeologyResourceRequest request = new IdeologyResourceRequest();
            request.setTitle(title);
            request.setResourceType(resourceType);
            request.setContentSummary(contentSummary);
            request.setCourseId(courseId);
            request.setValueTheme(valueTheme);
            request.setApplicableScene(applicableScene);
            request.setKeywords(keywords);
            request.setDifficulty(difficulty);
            request.setStatus(status);
            request.setAutoAnalyze(autoAnalyze);

            if (file != null && !file.isEmpty()) {
                String savedPath = saveFile(file);
                request.setSourceUrl(savedPath);
            }

            return Result.success(ideologyResourceService.create(request));
        } catch (Exception e) {
            return Result.error(500, "创建失败：" + e.getMessage());
        }
    }

    @PutMapping("/{resourceId:\\d+}/upload")
    public Result<IdeologyResource> updateWithFile(
            @PathVariable Long resourceId,
            @RequestParam("title") String title,
            @RequestParam(value = "resourceType", required = false, defaultValue = "document") String resourceType,
            @RequestParam(value = "contentSummary", required = false) String contentSummary,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "valueTheme", required = false) String valueTheme,
            @RequestParam(value = "applicableScene", required = false) String applicableScene,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "difficulty", required = false, defaultValue = "medium") String difficulty,
            @RequestParam(value = "status", required = false, defaultValue = "draft") String status,
            @RequestParam(value = "autoAnalyze", required = false, defaultValue = "false") Boolean autoAnalyze,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            IdeologyResourceRequest request = new IdeologyResourceRequest();
            request.setTitle(title);
            request.setResourceType(resourceType);
            request.setContentSummary(contentSummary);
            request.setCourseId(courseId);
            request.setValueTheme(valueTheme);
            request.setApplicableScene(applicableScene);
            request.setKeywords(keywords);
            request.setDifficulty(difficulty);
            request.setStatus(status);
            request.setAutoAnalyze(autoAnalyze);

            if (file != null && !file.isEmpty()) {
                String savedPath = saveFile(file);
                request.setSourceUrl(savedPath);
            }

            return Result.success(ideologyResourceService.update(resourceId, request));
        } catch (Exception e) {
            return Result.error(500, "更新失败：" + e.getMessage());
        }
    }

    private String saveFile(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        Path uploadPath = Paths.get(uploadDir, "ideology").toAbsolutePath();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(newFilename);
        file.transferTo(targetPath);

        return "/uploads/ideology/" + newFilename;
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
