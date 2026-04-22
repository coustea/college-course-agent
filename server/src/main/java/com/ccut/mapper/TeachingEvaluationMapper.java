package com.ccut.mapper;

import com.ccut.entity.TeachingEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 教学评价Mapper接口
 */
@Mapper
public interface TeachingEvaluationMapper {

    /**
     * 插入评价记录
     */
    int insert(TeachingEvaluation evaluation);

    /**
     * 更新评价记录
     */
    int updateById(TeachingEvaluation evaluation);

    /**
     * 根据ID查询
     */
    TeachingEvaluation selectById(Long evaluationId);

    /**
     * 根据ID删除
     */
    int deleteById(Long evaluationId);

    /**
     * 查询课程评价列表
     */
    List<TeachingEvaluation> selectByCourseId(@Param("courseId") Long courseId,
                                               @Param("period") String period);

    /**
     * 查询学生评价列表
     */
    List<TeachingEvaluation> selectByStudentId(@Param("studentId") Long studentId,
                                                @Param("courseId") Long courseId);

    /**
     * 查询学生特定课程最新评价
     */
    TeachingEvaluation selectLatestByStudentCourse(@Param("studentId") Long studentId,
                                                    @Param("courseId") Long courseId);

    /**
     * 查询课程平均评分
     */
    Double selectAvgScoreByCourseId(@Param("courseId") Long courseId,
                                     @Param("period") String period);

    /**
     * 统计课程评价数量
     */
    int countByCourseId(@Param("courseId") Long courseId,
                        @Param("period") String period);

    /**
     * 批量插入评价
     */
    int batchInsert(@Param("list") List<TeachingEvaluation> list);
}