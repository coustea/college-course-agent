package com.ccut.mapper;

import com.ccut.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterMapper {
    /**
     * 插入章节
     */
    int insert(Chapter chapter);

    /**
     * 更新章节
     */
    int updateById(Chapter chapter);

    /**
     * 删除章节
     */
    int deleteById(@Param("chapterId") Long chapterId);

    /**
     * 根据ID查询章节
     */
    Chapter findById(@Param("chapterId") Long chapterId);

    /**
     * 查询课程的所有一级章
     */
    List<Chapter> findChaptersByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询某章下的所有小节
     */
    List<Chapter> findSectionsByParentId(@Param("parentId") Long parentId);

    /**
     * 查询课程的完整章节树（包含章和节）
     */
    List<Chapter> findChapterTreeByCourseId(@Param("courseId") Long courseId);

    /**
     * 获取课程下一级章的最大序号
     */
    Integer findMaxChapterIndex(@Param("courseId") Long courseId);

    /**
     * 获取某章下小节的最大序号
     */
    Integer findMaxSectionIndex(@Param("parentId") Long parentId);

    /**
     * 更新章节序号
     */
    int updateChapterIndex(@Param("chapterId") Long chapterId, @Param("chapterIndex") Integer chapterIndex);

    /**
     * 批量更新章节序号
     */
    int batchUpdateChapterIndex(@Param("chapters") List<Chapter> chapters);
}
