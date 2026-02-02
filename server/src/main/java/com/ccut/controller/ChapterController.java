package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Chapter;
import com.ccut.service.ChapterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程章节控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/chapter")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    /**
     * 创建章
     */
    @PostMapping("/create")
    public Result<Chapter> createChapter(
            @RequestParam("courseId") Long courseId,
            @RequestParam("title") String title
    ) {
        try {
            return Result.success(chapterService.createChapter(courseId, title));
        } catch (IllegalArgumentException e) {
            log.warn("创建章参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建章异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 创建节
     */
    @PostMapping("/section/create")
    public Result<Chapter> createSection(
            @RequestParam("parentId") Long parentId,
            @RequestParam("title") String title
    ) {
        try {
            return Result.success(chapterService.createSection(parentId, title));
        } catch (IllegalArgumentException e) {
            log.warn("创建节参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建节异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新章节标题
     */
    @PutMapping("/update")
    public Result<Chapter> updateTitle(
            @RequestParam("chapterId") Long chapterId,
            @RequestParam("title") String title
    ) {
        try {
            return Result.success(chapterService.updateTitle(chapterId, title));
        } catch (IllegalArgumentException e) {
            log.warn("更新章节标题参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新章节标题异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/delete")
    public Result<String> deleteChapter(@RequestParam("chapterId") Long chapterId) {
        try {
            return Result.success(chapterService.deleteChapter(chapterId));
        } catch (IllegalArgumentException e) {
            log.warn("删除章节参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("删除章节异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取课程章节树
     */
    @GetMapping("/tree")
    public Result<List<Chapter>> getChapterTree(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(chapterService.getChapterTree(courseId));
        } catch (Exception e) {
            log.error("获取章节树异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 重新排序章节
     */
    @PutMapping("/reorder")
    public Result<String> reorderChapters(@RequestBody List<Chapter> chapters) {
        try {
            return Result.success(chapterService.reorderChapters(chapters));
        } catch (Exception e) {
            log.error("重新排序章节异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 为节关联视频
     */
    @PostMapping("/attach/video")
    public Result<String> attachVideo(
            @RequestParam("chapterId") Long chapterId,
            @RequestParam("videoId") Long videoId
    ) {
        try {
            return Result.success(chapterService.attachVideo(chapterId, videoId));
        } catch (Exception e) {
            log.error("关联视频异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 为节关联文档
     */
    @PostMapping("/attach/document")
    public Result<String> attachDocument(
            @RequestParam("chapterId") Long chapterId,
            @RequestParam("documentId") Long documentId
    ) {
        try {
            return Result.success(chapterService.attachDocument(chapterId, documentId));
        } catch (Exception e) {
            log.error("关联文档异常", e);
            return Result.error(500, e.getMessage());
        }
    }
}
