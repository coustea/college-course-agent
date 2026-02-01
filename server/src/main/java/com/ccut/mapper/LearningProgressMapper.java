package com.ccut.mapper;

import com.ccut.dto.CourseStatistics;
import com.ccut.entity.LearningProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningProgressMapper {

    int upsert(@Param("studentId") Long studentId,
               @Param("courseId") Long courseId,
               @Param("percent") Double percent,
               @Param("timeSpent") Integer timeSpent,
               @Param("completed") Boolean completed);

    LearningProgress findOne(@Param("studentId") Long studentId,
                             @Param("courseId") Long courseId);
    Double calcCoursePercent(@Param("studentId") Long studentId,
                             @Param("courseId") Long courseId);
    Boolean isCourseCompleted(@Param("studentId") Long studentId,
                              @Param("courseId") Long courseId);

    /**
     * 获取指定教师的所有已发布课程的统计数据（包含平均完成率）
     * 只统计选课了该课程的学生数据
     * @param teacherId 教师ID
     * @return 课程统计数据列表
     */
    List<CourseStatistics> getAllCourseStatistics(@Param("teacherId") Long teacherId);

    /**
     * 获取学生本周学习时长（秒）
     * @param studentId 学生ID
     * @return 本周学习时长（秒）
     */
    Integer getWeeklyStudyTime(@Param("studentId") Long studentId);

    /**
     * 获取学生连续打卡天数
     * @param studentId 学生ID
     * @return 连续打卡天数
     */
    Integer getConsecutiveDays(@Param("studentId") Long studentId);

    /**
     * 获取学生正在修读的课程数量
     * @param studentId 学生ID
     * @return 在修课程数量
     */
    Integer getEnrolledCourseCount(@Param("studentId") Long studentId);
}


