package com.ccut.mapper;

import com.ccut.entity.AiExamAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiExamAttemptMapper {
    int insert(AiExamAttempt attempt);
    AiExamAttempt selectById(@Param("id") Long id);
    int updateScore(@Param("id") Long id, @Param("score") Integer score);

    // 统计：按学生+课程计算正确率（正确题数/总题数），返回 0~1 的小数
    Double calcAccuracyByStudentAndCourse(@Param("studentId") Long studentId,
                                          @Param("courseId") Long courseId);
}




