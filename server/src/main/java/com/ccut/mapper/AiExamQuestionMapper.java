package com.ccut.mapper;

import com.ccut.entity.AiExamQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiExamQuestionMapper {
    int insert(AiExamQuestion q);
    List<AiExamQuestion> listByExamId(@Param("examId") Long examId);
}



