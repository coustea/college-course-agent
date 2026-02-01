package com.ccut.mapper;

import com.ccut.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 错题本 Mapper 接口
 */
@Mapper
public interface WrongQuestionMapper {

    /**
     * 插入错题记录
     */
    int insert(WrongQuestion wrongQuestion);

    /**
     * 根据学生ID和题目ID查询错题记录
     */
    WrongQuestion selectByStudentIdAndQuestionId(@Param("studentId") Long studentId,
                                                  @Param("questionId") Long questionId);

    /**
     * 根据学生ID查询错题列表（可按课程ID筛选）
     */
    List<WrongQuestion> selectByStudentId(@Param("studentId") Long studentId,
                                          @Param("courseId") Long courseId);

    /**
     * 根据ID查询错题详情
     */
    WrongQuestion selectById(@Param("id") Long id);

    /**
     * 更新错题次数和时间
     */
    int updateWrongCount(@Param("id") Long id);

    /**
     * 复习答对：增加连续答对次数
     */
    int incrementCorrectCount(@Param("id") Long id);

    /**
     * 复习答错：重置连续答对次数为0
     */
    int resetCorrectCount(@Param("id") Long id);

    /**
     * 标记为已掌握
     */
    int markAsMastered(@Param("id") Long id);

    /**
     * 取消掌握标记
     */
    int cancelMastered(@Param("id") Long id);

    /**
     * 删除错题记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计学生的错题总数
     */
    int countByStudentId(@Param("studentId") Long studentId,
                         @Param("courseId") Long courseId);

    /**
     * 统计学生已掌握的错题数
     */
    int countMasteredByStudentId(@Param("studentId") Long studentId,
                                  @Param("courseId") Long courseId);

    /**
     * 批量查询错题详情（包含题目内容）
     */
    List<WrongQuestion> selectDetailByStudentId(@Param("studentId") Long studentId,
                                                 @Param("courseId") Long courseId);
}
