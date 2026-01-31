package com.ccut.mapper;

import com.ccut.entity.AiExamAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiExamAnswerMapper {
    int insert(AiExamAnswer a);
    int updateCorrect(@Param("id") Long id, @Param("correct") Boolean correct);
}



