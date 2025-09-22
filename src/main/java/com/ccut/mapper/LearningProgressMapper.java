package com.ccut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ccut.entity.LearningProgress;

@Mapper
public interface LearningProgressMapper {

    int upsert(@Param("studentId") Long studentId,
               @Param("courseId") Long courseId,
               @Param("percent") Double percent,
               @Param("timeSpent") Integer timeSpent,
               @Param("completed") Boolean completed);

    LearningProgress findOne(@Param("studentId") Long studentId,
                             @Param("courseId") Long courseId);
    Double calcCoursePercent(@Param("studentId") Long studentId,
                             @Param("courseId") Long courseId);
    Boolean isCourseCompleted(@Param("studentId") Long studentId,
                              @Param("courseId") Long courseId);
}


