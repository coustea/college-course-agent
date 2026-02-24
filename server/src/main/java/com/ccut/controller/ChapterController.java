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
        log.debug("收到创建章请求：URI=/api/chapter/create, 参数：courseId={}, title={}", courseId, title);
        try {
            log.info("执行创建章业务：courseId={}, title={}", courseId, title);
            Chapter chapter = chapterService.createChapter(courseId, title);
            log.debug("创建章成功：courseId={}, chapterId={}", courseId, chapter.getChapterId());
            return Result.success(chapter);
        } catch (IllegalArgumentException e) {
            log.warn("创建章参数错误：courseId={}, title={}, 错误：{}", courseId, title, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建章异常：courseId={}, title={}, 错误：{}", courseId, title, e.getMessage(), e);
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
        log.debug("收到创建节请求：URI=/api/chapter/section/create, 参数：parentId={}, title={}", parentId, title);
        try {
            log.info("执行创建节业务：parentId={}, title={}", parentId, title);
            Chapter chapter = chapterService.createSection(parentId, title);
            log.debug("创建节成功：parentId={}, sectionId={}", parentId, chapter.getChapterId());
            return Result.success(chapter);
        } catch (IllegalArgumentException e) {
            log.warn("创建节参数错误：parentId={}, title={}, 错误：{}", parentId, title, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建节异常：parentId={}, title={}, 错误：{}", parentId, title, e.getMessage(), e);
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
        log.debug("收到更新章节标题请求：URI=/api/chapter/update, 参数：chapterId={}, title={}", chapterId, title);
        try {
            log.info("执行更新章节标题业务：chapterId={}, title={}", chapterId, title);
            Chapter chapter = chapterService.updateTitle(chapterId, title);
            log.debug("更新章节标题成功：chapterId={}", chapterId);
            return Result.success(chapter);
        } catch (IllegalArgumentException e) {
            log.warn("更新章节标题参数错误：chapterId={}, title={}, 错误：{}", chapterId, title, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新章节标题异常：chapterId={}, title={}, 错误：{}", chapterId, title, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/delete")
    public Result<String> deleteChapter(@RequestParam("chapterId") Long chapterId) {
        log.debug("收到删除章节请求：URI=/api/chapter/delete, 参数：chapterId={}", chapterId);
        try {
            log.info("执行删除章节业务：chapterId={}", chapterId);
            String result = chapterService.deleteChapter(chapterId);
            log.debug("删除章节成功：chapterId={}", chapterId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("删除章节参数错误：chapterId={}, 错误：{}", chapterId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("删除章节异常：chapterId={}, 错误：{}", chapterId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取课程章节树
     */
    @GetMapping("/tree")
    public Result<List<Chapter>> getChapterTree(@RequestParam("courseId") Long courseId) {
        log.debug("收到获取章节树请求：URI=/api/chapter/tree, 参数：courseId={}", courseId);
        try {
            log.info("执行获取章节树业务：courseId={}", courseId);
            List<Chapter> chapters = chapterService.getChapterTree(courseId);
            log.debug("获取章节树成功：courseId={}, 章节数={}", courseId, chapters.size());
            return Result.success(chapters);
        } catch (Exception e) {
            log.error("获取章节树异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 重新排序章节
     */
    @PutMapping("/reorder")
    public Result<String> reorderChapters(@RequestBody List<Chapter> chapters) {
        log.debug("收到重新排序章节请求：URI=/api/chapter/reorder, 参数：chapters 大小={}", chapters != null ? chapters.size() : 0);
        try {
            log.info("执行重新排序章节业务：chapters 大小={}", chapters != null ? chapters.size() : 0);
            String result = chapterService.reorderChapters(chapters);
            log.debug("重新排序章节成功");
            return Result.success(result);
        } catch (Exception e) {
            log.error("重新排序章节异常：错误：{}", e.getMessage(), e);
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
        log.debug("收到关联视频请求：URI=/api/chapter/attach/video, 参数：chapterId={}, videoId={}", chapterId, videoId);
        try {
            log.info("执行关联视频业务：chapterId={}, videoId={}", chapterId, videoId);
            String result = chapterService.attachVideo(chapterId, videoId);
            log.debug("关联视频成功：chapterId={}, videoId={}", chapterId, videoId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("关联视频异常：chapterId={}, videoId={}, 错误：{}", chapterId, videoId, e.getMessage(), e);
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
        log.debug("收到关联文档请求：URI=/api/chapter/attach/document, 参数：chapterId={}, documentId={}", chapterId, documentId);
        try {
            log.info("执行关联文档业务：chapterId={}, documentId={}", chapterId, documentId);
            String result = chapterService.attachDocument(chapterId, documentId);
            log.debug("关联文档成功：chapterId={}, documentId={}", chapterId, documentId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("关联文档异常：chapterId={}, documentId={}, 错误：{}", chapterId, documentId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
