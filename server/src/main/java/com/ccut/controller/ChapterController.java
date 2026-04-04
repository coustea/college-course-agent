package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Chapter;
import com.ccut.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程章节控制器
 */
@RestController
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
        Chapter chapter = chapterService.createChapter(courseId, title);
        return Result.success(chapter);
    }

    /**
     * 创建节
     */
    @PostMapping("/section/create")
    public Result<Chapter> createSection(
            @RequestParam("parentId") Long parentId,
            @RequestParam("title") String title
    ) {
        Chapter chapter = chapterService.createSection(parentId, title);
        return Result.success(chapter);
    }

    /**
     * 更新章节标题
     */
    @PutMapping("/update")
    public Result<Chapter> updateTitle(
            @RequestParam("chapterId") Long chapterId,
            @RequestParam("title") String title
    ) {
        Chapter chapter = chapterService.updateTitle(chapterId, title);
        return Result.success(chapter);
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/delete")
    public Result<String> deleteChapter(@RequestParam("chapterId") Long chapterId) {
        String result = chapterService.deleteChapter(chapterId);
        return Result.success(result);
    }

    /**
     * 获取课程章节树
     */
    @GetMapping("/tree")
    public Result<List<Chapter>> getChapterTree(@RequestParam("courseId") Long courseId) {
        List<Chapter> chapters = chapterService.getChapterTree(courseId);
        return Result.success(chapters);
    }

    /**
     * 重新排序章节
     */
    @PutMapping("/reorder")
    public Result<String> reorderChapters(@RequestBody List<Chapter> chapters) {
        String result = chapterService.reorderChapters(chapters);
        return Result.success(result);
    }

    /**
     * 为节关联视频
     */
    @PostMapping("/attach/video")
    public Result<String> attachVideo(
            @RequestParam("chapterId") Long chapterId,
            @RequestParam("videoId") Long videoId
    ) {
        String result = chapterService.attachVideo(chapterId, videoId);
        return Result.success(result);
    }

    /**
     * 为节关联文档
     */
    @PostMapping("/attach/document")
    public Result<String> attachDocument(
            @RequestParam("chapterId") Long chapterId,
            @RequestParam("documentId") Long documentId
    ) {
        String result = chapterService.attachDocument(chapterId, documentId);
        return Result.success(result);
    }
}
