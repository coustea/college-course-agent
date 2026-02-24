package com.ccut.service.Impl;

import com.ccut.entity.Chapter;
import com.ccut.mapper.ChapterMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.ChapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 课程章节服务实现类
 */
@Service
public class ChapterServiceImpl implements ChapterService {

    private static final Logger log = LoggerFactory.getLogger(ChapterServiceImpl.class);

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Override
    @Transactional
    public Chapter createChapter(Long courseId, String title) {
        log.debug("执行方法：createChapter, 参数：courseId={}, title={}", courseId, title);
        long startTime = System.currentTimeMillis();
        try {
            if (courseId == null) {
                log.error("参数验证失败：课程 ID 不能为空");
                throw new IllegalArgumentException("课程 ID 不能为空");
            }
            if (title == null || title.trim().isEmpty()) {
                log.error("参数验证失败：章节标题不能为空");
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
            long duration = System.currentTimeMillis() - startTime;
            log.info("创建章节成功：chapterId={}, courseId={}, title={}, 耗时={}ms", 
                    chapter.getChapterId(), courseId, title, duration);
            log.debug("方法返回：result={}", chapter);
            return chapter;
        } catch (Exception e) {
            log.error("创建章节失败：courseId={}, title={}, error={}", courseId, title, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Chapter createSection(Long parentId, String title) {
        log.debug("执行方法：createSection, 参数：parentId={}, title={}", parentId, title);
        long startTime = System.currentTimeMillis();
        try {
            if (parentId == null) {
                log.error("参数验证失败：父章节 ID 不能为空");
                throw new IllegalArgumentException("父章节 ID 不能为空");
            }
            if (title == null || title.trim().isEmpty()) {
                log.error("参数验证失败：小节标题不能为空");
                throw new IllegalArgumentException("小节标题不能为空");
            }

            // 验证父章节存在
            Chapter parent = chapterMapper.findById(parentId);
            if (parent == null) {
                log.error("父章节不存在：parentId={}", parentId);
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
            long duration = System.currentTimeMillis() - startTime;
            log.info("创建小节成功：sectionId={}, parentId={}, title={}, 耗时={}ms", 
                    section.getChapterId(), parentId, title, duration);
            log.debug("方法返回：result={}", section);
            return section;
        } catch (Exception e) {
            log.error("创建小节失败：parentId={}, title={}, error={}", parentId, title, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Chapter updateTitle(Long chapterId, String title) {
        log.debug("执行方法：updateTitle, 参数：chapterId={}, title={}", chapterId, title);
        long startTime = System.currentTimeMillis();
        try {
            if (chapterId == null) {
                log.error("参数验证失败：章节 ID 不能为空");
                throw new IllegalArgumentException("章节 ID 不能为空");
            }
            if (title == null || title.trim().isEmpty()) {
                log.error("参数验证失败：章节标题不能为空");
                throw new IllegalArgumentException("章节标题不能为空");
            }

            Chapter chapter = chapterMapper.findById(chapterId);
            if (chapter == null) {
                log.error("章节不存在：chapterId={}", chapterId);
                throw new IllegalArgumentException("章节不存在");
            }

            chapter.setTitle(title.trim());
            chapterMapper.updateById(chapter);
            long duration = System.currentTimeMillis() - startTime;
            log.info("更新章节标题成功：chapterId={}, newTitle={}, 耗时={}ms", chapterId, title, duration);
            log.debug("方法返回：result={}", chapter);
            return chapter;
        } catch (Exception e) {
            log.error("更新章节标题失败：chapterId={}, title={}, error={}", chapterId, title, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String deleteChapter(Long chapterId) {
        log.debug("执行方法：deleteChapter, 参数：chapterId={}", chapterId);
        long startTime = System.currentTimeMillis();
        try {
            if (chapterId == null) {
                log.error("参数验证失败：章节 ID 不能为空");
                throw new IllegalArgumentException("章节 ID 不能为空");
            }

            Chapter chapter = chapterMapper.findById(chapterId);
            if (chapter == null) {
                log.error("章节不存在：chapterId={}", chapterId);
                throw new IllegalArgumentException("章节不存在");
            }

            // 如果是章，需要先删除其下所有节
            if ("chapter".equals(chapter.getChapterType())) {
                List<Chapter> sections = chapterMapper.findSectionsByParentId(chapterId);
                log.info("删除章及其下所有节：chapterId={}, sectionsCount={}", chapterId, sections != null ? sections.size() : 0);
                for (Chapter section : sections) {
                    deleteChapter(section.getChapterId());
                }
            }

            chapterMapper.deleteById(chapterId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("删除章节成功：chapterId={}, 耗时={}ms", chapterId, duration);
            log.debug("方法返回：result=删除成功");
            return "删除成功";
        } catch (Exception e) {
            log.error("删除章节失败：chapterId={}, error={}", chapterId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Chapter> getChapterTree(Long courseId) {
        log.debug("执行方法：getChapterTree, 参数：courseId={}", courseId);
        long startTime = System.currentTimeMillis();
        try {
            if (courseId == null) {
                log.error("参数验证失败：课程 ID 不能为空");
                throw new IllegalArgumentException("课程 ID 不能为空");
            }
            List<Chapter> result = chapterMapper.findChapterTreeByCourseId(courseId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询章节树成功：courseId={}, count={}, 耗时={}ms", courseId, result != null ? result.size() : 0, duration);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询章节树失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String reorderChapters(List<Chapter> chapters) {
        log.debug("执行方法：reorderChapters, 参数：chapters count={}", chapters != null ? chapters.size() : 0);
        long startTime = System.currentTimeMillis();
        try {
            if (chapters == null || chapters.isEmpty()) {
                log.error("参数验证失败：章节列表不能为空");
                throw new IllegalArgumentException("章节列表不能为空");
            }

            for (Chapter chapter : chapters) {
                if (chapter.getChapterId() != null && chapter.getChapterIndex() != null) {
                    chapterMapper.updateChapterIndex(chapter.getChapterId(), chapter.getChapterIndex());
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("章节排序更新成功：count={}, 耗时={}ms", chapters.size(), duration);
            log.debug("方法返回：result=排序更新成功");
            return "排序更新成功";
        } catch (Exception e) {
            log.error("章节排序更新失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String attachVideo(Long chapterId, Long videoId) {
        log.debug("执行方法：attachVideo, 参数：chapterId={}, videoId={}", chapterId, videoId);
        long startTime = System.currentTimeMillis();
        try {
            if (chapterId == null || videoId == null) {
                log.error("参数验证失败：章节 ID 和视频 ID 不能为空");
                throw new IllegalArgumentException("章节 ID 和视频 ID 不能为空");
            }

            Chapter chapter = chapterMapper.findById(chapterId);
            if (chapter == null) {
                log.error("章节不存在：chapterId={}", chapterId);
                throw new IllegalArgumentException("章节不存在");
            }

            // TODO: 更新 course_videos 表的 chapter_id 字段
            // 这里需要先创建 CourseVideoMapper 的更新方法

            chapter.setContentType(1); // 视频内容
            chapterMapper.updateById(chapter);
            long duration = System.currentTimeMillis() - startTime;
            log.info("视频关联成功：chapterId={}, videoId={}, 耗时={}ms", chapterId, videoId, duration);
            log.debug("方法返回：result=视频关联成功");
            return "视频关联成功";
        } catch (Exception e) {
            log.error("视频关联失败：chapterId={}, videoId={}, error={}", chapterId, videoId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String attachDocument(Long chapterId, Long documentId) {
        log.debug("执行方法：attachDocument, 参数：chapterId={}, documentId={}", chapterId, documentId);
        long startTime = System.currentTimeMillis();
        try {
            if (chapterId == null || documentId == null) {
                log.error("参数验证失败：章节 ID 和文档 ID 不能为空");
                throw new IllegalArgumentException("章节 ID 和文档 ID 不能为空");
            }

            Chapter chapter = chapterMapper.findById(chapterId);
            if (chapter == null) {
                log.error("章节不存在：chapterId={}", chapterId);
                throw new IllegalArgumentException("章节不存在");
            }

            // TODO: 更新 course_documents 表的 chapter_id 字段
            // 这里需要先创建 CourseDocumentMapper 的更新方法

            chapter.setContentType(2); // 文档内容
            chapterMapper.updateById(chapter);
            long duration = System.currentTimeMillis() - startTime;
            log.info("文档关联成功：chapterId={}, documentId={}, 耗时={}ms", chapterId, documentId, duration);
            log.debug("方法返回：result=文档关联成功");
            return "文档关联成功";
        } catch (Exception e) {
            log.error("文档关联失败：chapterId={}, documentId={}, error={}", chapterId, documentId, e.getMessage(), e);
            throw e;
        }
    }
}
