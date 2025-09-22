package com.ccut.mapper;

import com.ccut.entity.AiExam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiExamMapper {
    int insert(AiExam exam);
    int updateScoreAndStatus(@Param("id") Long id,
                             @Param("totalScore") Integer totalScore,
                             @Param("status") String status);
    AiExam selectById(@Param("id") Long id);
}



