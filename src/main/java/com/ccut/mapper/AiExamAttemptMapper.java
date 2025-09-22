package com.ccut.mapper;

import com.ccut.entity.AiExamAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiExamAttemptMapper {
    int insert(AiExamAttempt attempt);
    AiExamAttempt selectById(@Param("id") Long id);
    int updateScore(@Param("id") Long id, @Param("score") Integer score);
}




