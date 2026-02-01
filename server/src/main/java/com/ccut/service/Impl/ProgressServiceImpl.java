package com.ccut.service.Impl;

import com.ccut.dto.CourseStatistics;
import com.ccut.dto.StudentStatistics;
import com.ccut.entity.CourseDocument;
import com.ccut.entity.CourseVideo;
import com.ccut.entity.DocumentProgress;
import com.ccut.entity.LearningProgress;
import com.ccut.entity.VideoProgress;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.DocumentProgressMapper;
import com.ccut.mapper.LearningProgressMapper;
import com.ccut.mapper.VideoProgressMapper;
import com.ccut.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习进度服务实现类
 */
@Service
public class ProgressServiceImpl implements ProgressService {

    @Autowired
    private LearningProgressMapper learningProgressMapper;

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private DocumentProgressMapper documentProgressMapper;

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Override
    @Transactional
    public void reportProgress(Long studentId, Long courseId, Long videoId, Long documentId,
                               Integer deltaSec, Double scrollPct, Boolean completed) {
        if (deltaSec == null || deltaSec < 0) deltaSec = 0;
        if (scrollPct == null) scrollPct = 0.0;

        if (videoId == null && documentId == null) {
            throw new IllegalArgumentException("需要提供 videoId 或 documentId 之一");
        }
        if (videoId != null && documentId != null) {
            throw new IllegalArgumentException("videoId 与 documentId 不能同时提供");
        }

        // 先更新子项维度
        if (videoId != null) {
            videoProgressMapper.upsert(studentId, courseId, videoId, deltaSec, completed);
        } else {
            documentProgressMapper.upsert(studentId, courseId, documentId, deltaSec, scrollPct, completed);
        }

        // 重新计算课程层的百分比与完成（由 SQL 聚合：视频+文档个数完成度）
        // 这里仍然保留 time_spent 的累计字段，用于历史兼容或展示
        learningProgressMapper.upsert(studentId, courseId, 0.0, deltaSec, completed);
    }

    @Override
    public LearningProgress getCourseProgress(Long studentId, Long courseId) {
        LearningProgress lp = learningProgressMapper.findOne(studentId, courseId);
        Double pct = learningProgressMapper.calcCoursePercent(studentId, courseId);
        if (pct == null) pct = 0.0;
        boolean courseCompleted = Boolean.TRUE.equals(learningProgressMapper.isCourseCompleted(studentId, courseId));
        if (lp == null) {
            lp = new LearningProgress(null, studentId, courseId, courseCompleted, pct, 0, null);
        } else {
            lp.setCompleted(courseCompleted);
            lp.setCompletionPercentage(courseCompleted ? 100.0 : pct);
        }
        return lp;
    }

    @Override
    public VideoProgress getVideoProgress(Long studentId, Long courseId, Long videoId) {
        return videoProgressMapper.findOne(studentId, courseId, videoId);
    }

    @Override
    public List<VideoProgress> listVideoProgress(Long studentId, Long courseId) {
        return videoProgressMapper.listByCourse(studentId, courseId);
    }

    @Override
    public DocumentProgress getDocumentProgress(Long studentId, Long courseId, Long documentId) {
        return documentProgressMapper.findOne(studentId, courseId, documentId);
    }

    @Override
    public List<DocumentProgress> listDocumentProgress(Long studentId, Long courseId) {
        return documentProgressMapper.listByCourse(studentId, courseId);
    }

    @Override
    public Map<String, Object> getAllProgress(Long studentId, Long courseId) {
        // 1. 获取学生已有的进度记录
        List<VideoProgress> studentVideos = videoProgressMapper.listByCourse(studentId, courseId);
        List<DocumentProgress> studentDocs = documentProgressMapper.listByCourse(studentId, courseId);

        // 2. 获取课程的全部视频与文档
        List<CourseVideo> allVideos = courseVideoMapper.findByCourseId(courseId);
        List<CourseDocument> allDocs = courseDocumentMapper.findByCourseId(courseId);

        // 3. 构建已有进度的映射（按 videoId/documentId 作为 key）
        Map<Long, VideoProgress> vpMap = new HashMap<>();
        for (VideoProgress vp : studentVideos) {
            if (vp.getVideoId() != null) vpMap.put(vp.getVideoId(), vp);
        }
        Map<Long, DocumentProgress> dpMap = new HashMap<>();
        for (DocumentProgress dp : studentDocs) {
            if (dp.getDocumentId() != null) dpMap.put(dp.getDocumentId(), dp);
        }

        // 4. 合并：对所有视频，如果学生有进度则用进度，否则构造 0% 占位
        List<VideoProgress> mergedVideos = new ArrayList<>();
        for (CourseVideo cv : allVideos) {
            Long vid = cv.getVideoId();
            if (vpMap.containsKey(vid)) {
                mergedVideos.add(vpMap.get(vid));
            } else {
                VideoProgress placeholder = new VideoProgress();
                placeholder.setId(0L); // 占位ID，表示无实际进度记录
                placeholder.setVideoId(vid);
                placeholder.setStudentId(studentId);
                placeholder.setCourseId(courseId);
                placeholder.setWatchedSeconds(0);
                placeholder.setCompleted(false);
                placeholder.setPercentage(0.0);
                placeholder.setUpdatedAt(null);
                mergedVideos.add(placeholder);
            }
        }

        // 5. 合并文档
        List<DocumentProgress> mergedDocs = new ArrayList<>();
        for (CourseDocument cd : allDocs) {
            Long did = cd.getDocumentId();
            if (dpMap.containsKey(did)) {
                mergedDocs.add(dpMap.get(did));
            } else {
                DocumentProgress placeholder = new DocumentProgress();
                placeholder.setDocumentId(did);
                placeholder.setStudentId(studentId);
                placeholder.setCourseId(courseId);
                placeholder.setTimeSpent(0);
                placeholder.setMaxScrollPct(0.0);
                placeholder.setCompleted(false);
                placeholder.setPercentage(0.0);
                mergedDocs.add(placeholder);
            }
        }

        Double pct = learningProgressMapper.calcCoursePercent(studentId, courseId);
        if (pct == null) pct = 0.0;
        boolean courseCompleted = Boolean.TRUE.equals(learningProgressMapper.isCourseCompleted(studentId, courseId));

        Map<String, Object> resp = new HashMap<>();
        resp.put("videos", mergedVideos);
        resp.put("documents", mergedDocs);
        resp.put("coursePercent", courseCompleted ? 100.0 : pct);
        resp.put("courseCompleted", courseCompleted);
        return resp;
    }

    @Override
    public List<CourseStatistics> getAllCourseStatistics(Long teacherId) {
        return learningProgressMapper.getAllCourseStatistics(teacherId);
    }

    @Override
    public StudentStatistics getStudentStatistics(Long studentId) {
        // 获取在修课程数量
        Integer courseCount = learningProgressMapper.getEnrolledCourseCount(studentId);
        if (courseCount == null) courseCount = 0;

        // 获取本周学习时长（秒），转换为小时
        Integer weeklySeconds = learningProgressMapper.getWeeklyStudyTime(studentId);
        if (weeklySeconds == null) weeklySeconds = 0;
        Double weeklyHours = weeklySeconds / 3600.0;

        // 获取连续打卡天数
        Integer consecutiveDays = learningProgressMapper.getConsecutiveDays(studentId);
        if (consecutiveDays == null) consecutiveDays = 0;

        return new StudentStatistics(courseCount, weeklyHours, consecutiveDays);
    }
}
