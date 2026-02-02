package com.ccut.service;

import com.ccut.entity.Chapter;

import java.util.List;

/**
 * 课程章节服务接口
 */
public interface ChapterService {

    /**
     * 创建章
     * @param courseId 课程ID
     * @param title 章标题
     * @return 创建的章
     */
    Chapter createChapter(Long courseId, String title);

    /**
     * 创建节
     * @param parentId 父章节ID
     * @param title 节标题
     * @return 创建的节
     */
    Chapter createSection(Long parentId, String title);

    /**
     * 更新章节标题
     * @param chapterId 章节ID
     * @param title 新标题
     * @return 更新后的章节
     */
    Chapter updateTitle(Long chapterId, String title);

    /**
     * 删除章节（如果是章，会级联删除其下所有节）
     * @param chapterId 章节ID
     * @return 删除结果消息
     */
    String deleteChapter(Long chapterId);

    /**
     * 获取课程的章节树
     * @param courseId 课程ID
     * @return 章节树列表
     */
    List<Chapter> getChapterTree(Long courseId);

    /**
     * 移动章节（改变排序）
     * @param chapters 章节列表（包含新的序号）
     * @return 移动结果消息
     */
    String reorderChapters(List<Chapter> chapters);

    /**
     * 为节添加视频内容
     * @param chapterId 节ID
     * @param videoId 视频ID
     * @return 更新结果消息
     */
    String attachVideo(Long chapterId, Long videoId);

    /**
     * 为节添加文档内容
     * @param chapterId 节ID
     * @param documentId 文档ID
     * @return 更新结果消息
     */
    String attachDocument(Long chapterId, Long documentId);
}
