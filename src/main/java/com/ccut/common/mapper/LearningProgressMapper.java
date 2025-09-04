package com.ccut.common.mapper;

import com.ccut.common.entity.LearningProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LearningProgressMapper {

    int insert(LearningProgress learningProgress);

    int updateById(LearningProgress learningProgress);

    List<LearningProgress> findByStudentId(@Param("studentId") Long studentId);

    Optional<LearningProgress> findByStudentIdAndCourseId(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId);
}