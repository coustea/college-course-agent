package com.ccut.mapper;

import com.ccut.entity.SentimentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SentimentRecordMapper {
    int insert(SentimentRecord record);
    List<SentimentRecord> findByStudentAndCourseAfter(@Param("studentId") Long studentId,
                                                        @Param("courseId") Long courseId,
                                                        @Param("startTime") LocalDateTime startTime);
    List<SentimentRecord> findByCourseId(@Param("courseId") Long courseId, @Param("limit") Integer limit);
}
