package com.ccut.mapper;

import com.ccut.entity.Recommendation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecommendationMapper {

    @Insert("INSERT INTO recommendation (student_id, course_id, reason, score, recommendation_type, created_at, has_clicked) " +
            "VALUES (#{studentId}, #{courseId}, #{reason}, #{score}, #{recommendationType}, #{createdAt}, #{hasClicked})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Recommendation recommendation);

    @Select("SELECT * FROM recommendation WHERE student_id = #{studentId} ORDER BY score DESC LIMIT #{limit}")
    List<Recommendation> findByStudentId(@Param("studentId") Long studentId, @Param("limit") int limit);

    @Select("SELECT * FROM recommendation WHERE student_id = #{studentId} AND has_clicked = false")
    List<Recommendation> findUnreadByStudentId(@Param("studentId") Long studentId);

    @Update("UPDATE recommendation SET has_clicked = true, clicked_at = #{clickedAt} WHERE id = #{id}")
    int markAsClicked(@Param("id") Long id, @Param("clickedAt") java.time.LocalDateTime clickedAt);

    @Delete("DELETE FROM recommendation WHERE student_id = #{studentId} AND created_at < #{beforeDate}")
    int deleteOldRecommendations(@Param("studentId") Long studentId, @Param("beforeDate") java.time.LocalDateTime beforeDate);
}
