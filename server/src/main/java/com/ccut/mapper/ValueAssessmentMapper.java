package com.ccut.mapper;

import com.ccut.entity.ValueAssessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ValueAssessmentMapper {
    int insert(ValueAssessment assessment);
    ValueAssessment findLatestByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    List<ValueAssessment> findByStudentAndCourseAfter(@Param("studentId") Long studentId,
                                                       @Param("courseId") Long courseId,
                                                       @Param("startTime") LocalDateTime startTime);
    List<ValueAssessment> findByCourseId(@Param("courseId") Long courseId);
}
