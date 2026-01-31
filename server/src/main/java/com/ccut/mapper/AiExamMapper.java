package com.ccut.mapper;

import com.ccut.entity.AiExam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiExamMapper {
    int insert(AiExam exam);
    int updateScoreAndStatus(@Param("id") Long id,
                             @Param("totalScore") Integer totalScore,
                             @Param("status") String status);
    AiExam selectById(@Param("id") Long id);
    
    // 获取学生在某课程中所有题目数为5的考试的平均成绩
    Double getAverageScoreByStudentAndCourse(@Param("studentId") Long studentId,
                                             @Param("courseId") Long courseId);
    
    // 获取学生在某课程中每个视频的平均成绩（只统计submitted状态）
    List<Map<String, Object>> getVideoScoresByStudentAndCourse(@Param("studentId") Long studentId,
                                                                @Param("courseId") Long courseId);
    
    // 获取学生在某课程中每个文档的平均成绩（只统计submitted状态）
    List<Map<String, Object>> getDocumentScoresByStudentAndCourse(@Param("studentId") Long studentId,
                                                                   @Param("courseId") Long courseId);
    
    // 获取学生在某课程中的所有考试记录
    List<AiExam> listByStudentAndCourse(@Param("studentId") Long studentId,
                                        @Param("courseId") Long courseId);
}



