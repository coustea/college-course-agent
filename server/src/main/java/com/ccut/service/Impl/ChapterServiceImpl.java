package com.ccut.service.Impl;

import com.ccut.entity.Chapter;
import com.ccut.mapper.ChapterMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 课程章节服务实现类
 */
@Service
public class ChapterServiceImpl implements ChapterService {

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Override
    @Transactional
    public Chapter createChapter(Long courseId, String title) {
        if (courseId == null) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("章节标题不能为空");
        }

        // 获取当前课程的最大序号
        Integer maxIndex = chapterMapper.findMaxChapterIndex(courseId);
        int nextIndex = (maxIndex == null ? 0 : maxIndex) + 1;

        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setParentId(null);
        chapter.setTitle(title.trim());
        chapter.setChapterIndex(nextIndex);
        chapter.setChapterType("chapter");
        chapter.setContentType(0); // 无内容
        chapter.setSort(0);

        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public Chapter createSection(Long parentId, String title) {
        if (parentId == null) {
            throw new IllegalArgumentException("父章节ID不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("小节标题不能为空");
        }

        // 验证父章节存在
        Chapter parent = chapterMapper.findById(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父章节不存在");
        }

        // 获取当前父章节下的最大序号
        Integer maxIndex = chapterMapper.findMaxSectionIndex(parentId);
        int nextIndex = (maxIndex == null ? 0 : maxIndex) + 1;

        Chapter section = new Chapter();
        section.setCourseId(parent.getCourseId());
        section.setParentId(parentId);
        section.setTitle(title.trim());
        section.setChapterIndex(nextIndex);
        section.setChapterType("section");
        section.setContentType(0); // 无内容
        section.setSort(0);

        chapterMapper.insert(section);
        return section;
    }

    @Override
    @Transactional
    public Chapter updateTitle(Long chapterId, String title) {
        if (chapterId == null) {
            throw new IllegalArgumentException("章节ID不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("章节标题不能为空");
        }

        Chapter chapter = chapterMapper.findById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在");
        }

        chapter.setTitle(title.trim());
        chapterMapper.updateById(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public String deleteChapter(Long chapterId) {
        if (chapterId == null) {
            throw new IllegalArgumentException("章节ID不能为空");
        }

        Chapter chapter = chapterMapper.findById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在");
        }

        // 如果是章，需要先删除其下所有节
        if ("chapter".equals(chapter.getChapterType())) {
            List<Chapter> sections = chapterMapper.findSectionsByParentId(chapterId);
            for (Chapter section : sections) {
                deleteChapter(section.getChapterId());
            }
        }

        chapterMapper.deleteById(chapterId);
        return "删除成功";
    }

    @Override
    public List<Chapter> getChapterTree(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        return chapterMapper.findChapterTreeByCourseId(courseId);
    }

    @Override
    @Transactional
    public String reorderChapters(List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            throw new IllegalArgumentException("章节列表不能为空");
        }

        for (Chapter chapter : chapters) {
            if (chapter.getChapterId() != null && chapter.getChapterIndex() != null) {
                chapterMapper.updateChapterIndex(chapter.getChapterId(), chapter.getChapterIndex());
            }
        }
        return "排序更新成功";
    }

    @Override
    @Transactional
    public String attachVideo(Long chapterId, Long videoId) {
        if (chapterId == null || videoId == null) {
            throw new IllegalArgumentException("章节ID和视频ID不能为空");
        }

        Chapter chapter = chapterMapper.findById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在");
        }

        // TODO: 更新 course_videos 表的 chapter_id 字段
        // 这里需要先创建 CourseVideoMapper 的更新方法

        chapter.setContentType(1); // 视频内容
        chapterMapper.updateById(chapter);
        return "视频关联成功";
    }

    @Override
    @Transactional
    public String attachDocument(Long chapterId, Long documentId) {
        if (chapterId == null || documentId == null) {
            throw new IllegalArgumentException("章节ID和文档ID不能为空");
        }

        Chapter chapter = chapterMapper.findById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在");
        }

        // TODO: 更新 course_documents 表的 chapter_id 字段
        // 这里需要先创建 CourseDocumentMapper 的更新方法

        chapter.setContentType(2); // 文档内容
        chapterMapper.updateById(chapter);
        return "文档关联成功";
    }
}
