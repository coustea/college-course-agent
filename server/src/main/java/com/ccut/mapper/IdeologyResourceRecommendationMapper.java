package com.ccut.mapper;

import com.ccut.entity.IdeologyResourceRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface IdeologyResourceRecommendationMapper {

    int insert(IdeologyResourceRecommendation recommendation);

    List<IdeologyResourceRecommendation> findByStudentId(@Param("studentId") Long studentId,
                                                         @Param("limit") int limit);

    int markAsClicked(@Param("id") Long id, @Param("clickedAt") LocalDateTime clickedAt);

    int deleteByStudentId(@Param("studentId") Long studentId);
}
