package com.ccut.controller;

import com.ccut.entity.LearningProgress;
import com.ccut.entity.Result;
import com.ccut.mapper.LearningProgressMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.VideoProgressMapper;
import com.ccut.mapper.DocumentProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private LearningProgressMapper learningProgressMapper;
    @Autowired
    private CourseVideoMapper courseVideoMapper;
    @Autowired
    private VideoProgressMapper videoProgressMapper;
    @Autowired
    private DocumentProgressMapper documentProgressMapper;

    // 上报学习增量：支持 videoId 或 documentId
    @PostMapping("/report")
    public Result<String> report(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId,
                                 @RequestParam(value = "videoId", required = false) Long videoId,
                                 @RequestParam(value = "documentId", required = false) Long documentId,
                                 @RequestParam(value = "deltaSec", defaultValue = "0") Integer deltaSec,
                                 @RequestParam(value = "scrollPct", required = false) Double scrollPct,
                                 @RequestParam(value = "completed", defaultValue = "false") Boolean completed){
        try {
            if (deltaSec == null || deltaSec < 0) deltaSec = 0;
            if (scrollPct == null) scrollPct = 0.0;

            if (videoId == null && documentId == null) {
                return Result.error(400, "需要提供 videoId 或 documentId 之一");
            }
            if (videoId != null && documentId != null) {
                return Result.error(400, "videoId 与 documentId 不能同时提供");
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

            return Result.success("ok");
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 查询课程汇总进度
    @GetMapping("/course")
    public Result<LearningProgress> getCourseProgress(@RequestParam("studentId") Long studentId,
                                                      @RequestParam("courseId") Long courseId){
        try {
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
            return Result.success(lp);
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 查询某个视频的学习进度
    @GetMapping("/video")
    public Result<com.ccut.entity.VideoProgress> getVideoProgress(@RequestParam("studentId") Long studentId,
                                                                  @RequestParam("courseId") Long courseId,
                                                                  @RequestParam("videoId") Long videoId){
        try {
            return Result.success(videoProgressMapper.findOne(studentId, courseId, videoId));
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 列出一个课程下该生所有视频的进度
    @GetMapping("/video/list")
    public Result<java.util.List<com.ccut.entity.VideoProgress>> listVideoProgress(@RequestParam("studentId") Long studentId,
                                                                                   @RequestParam("courseId") Long courseId){
        try {
            return Result.success(videoProgressMapper.listByCourse(studentId, courseId));
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 查询某个文档的学习进度
    @GetMapping("/document")
    public Result<com.ccut.entity.DocumentProgress> getDocumentProgress(@RequestParam("studentId") Long studentId,
                                                                        @RequestParam("courseId") Long courseId,
                                                                        @RequestParam("documentId") Long documentId){
        try {
            return Result.success(documentProgressMapper.findOne(studentId, courseId, documentId));
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 列出一个课程下该生所有文档的进度
    @GetMapping("/document/list")
    public Result<java.util.List<com.ccut.entity.DocumentProgress>> listDocumentProgress(@RequestParam("studentId") Long studentId,
                                                                                          @RequestParam("courseId") Long courseId){
        try {
            return Result.success(documentProgressMapper.listByCourse(studentId, courseId));
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 聚合查询：课程下该生所有视频与文档的进度 + 汇总
    @GetMapping("/course/all")
    public Result<java.util.Map<String, Object>> getAllProgress(@RequestParam("studentId") Long studentId,
                                                                @RequestParam("courseId") Long courseId){
        try {
            java.util.List<com.ccut.entity.VideoProgress> videos = videoProgressMapper.listByCourse(studentId, courseId);
            java.util.List<com.ccut.entity.DocumentProgress> docs = documentProgressMapper.listByCourse(studentId, courseId);

            Double pct = learningProgressMapper.calcCoursePercent(studentId, courseId);
            if (pct == null) pct = 0.0;
            boolean courseCompleted = Boolean.TRUE.equals(learningProgressMapper.isCourseCompleted(studentId, courseId));

            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("videos", videos);
            resp.put("documents", docs);
            resp.put("coursePercent", courseCompleted ? 100.0 : pct);
            resp.put("courseCompleted", courseCompleted);
            return Result.success(resp);
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}


