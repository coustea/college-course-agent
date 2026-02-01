package com.ccut.service;

import com.ccut.dto.CourseStatistics;
import com.ccut.entity.DocumentProgress;
import com.ccut.entity.LearningProgress;
import com.ccut.entity.VideoProgress;

import java.util.List;
import java.util.Map;

/**
 * 学习进度服务接口
 */
public interface ProgressService {

    /**
     * 上报学习增量
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param videoId 视频ID（可选）
     * @param documentId 文档ID（可选）
     * @param deltaSec 学习时长增量（秒）
     * @param scrollPct 文档滚动百分比（可选）
     * @param completed 是否完成
     */
    void reportProgress(Long studentId, Long courseId, Long videoId, Long documentId,
                        Integer deltaSec, Double scrollPct, Boolean completed);

    /**
     * 查询课程汇总进度
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 课程学习进度
     */
    LearningProgress getCourseProgress(Long studentId, Long courseId);

    /**
     * 查询某个视频的学习进度
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param videoId 视频ID
     * @return 视频学习进度
     */
    VideoProgress getVideoProgress(Long studentId, Long courseId, Long videoId);

    /**
     * 列出一个课程下该生所有视频的进度
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 视频进度列表
     */
    List<VideoProgress> listVideoProgress(Long studentId, Long courseId);

    /**
     * 查询某个文档的学习进度
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param documentId 文档ID
     * @return 文档学习进度
     */
    DocumentProgress getDocumentProgress(Long studentId, Long courseId, Long documentId);

    /**
     * 列出一个课程下该生所有文档的进度
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 文档进度列表
     */
    List<DocumentProgress> listDocumentProgress(Long studentId, Long courseId);

    /**
     * 聚合查询：课程下该生所有视频与文档的进度 + 汇总（未学的节也以0%返回）
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 包含视频、文档进度和课程汇总的映射
     */
    Map<String, Object> getAllProgress(Long studentId, Long courseId);

    /**
     * 获取指定教师的所有已发布课程的统计数据（包含平均完成率）
     * @param teacherId 教师ID
     * @return 课程统计数据列表
     */
    List<CourseStatistics> getAllCourseStatistics(Long teacherId);

    /**
     * 获取学生统计数据
     * @param studentId 学生ID
     * @return 学生统计数据
     */
    com.ccut.dto.StudentStatistics getStudentStatistics(Long studentId);

}
