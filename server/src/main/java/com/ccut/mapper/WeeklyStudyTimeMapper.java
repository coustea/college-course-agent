package com.ccut.mapper;

import com.ccut.entity.WeeklyStudyTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 每周学习时间统计 Mapper接口
 */
@Mapper
public interface WeeklyStudyTimeMapper {

    /**
     * 累加学习时间（如果本周记录不存在则创建，存在则累加）
     * @param studentId 学生ID
     * @param courseId 课程ID（可为null，表示总学习时间）
     * @param weekStartDate 本周开始日期（周一）
     * @param deltaSeconds 增量秒数
     */
    void upsert(@Param("studentId") Long studentId,
                @Param("courseId") Long courseId,
                @Param("weekStartDate") Date weekStartDate,
                @Param("deltaSeconds") Integer deltaSeconds);

    /**
     * 获取学生本周总学习时间（所有课程）
     * @param studentId 学生ID
     * @param weekStartDate 本周开始日期
     * @return 本周学习秒数
     */
    Integer getStudentWeeklyTime(@Param("studentId") Long studentId,
                                 @Param("weekStartDate") Date weekStartDate);

    /**
     * 获取学生本周在指定课程的学习时间
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param weekStartDate 本周开始日期
     * @return 该课程本周学习秒数
     */
    Integer getStudentCourseWeeklyTime(@Param("studentId") Long studentId,
                                       @Param("courseId") Long courseId,
                                       @Param("weekStartDate") Date weekStartDate);

    /**
     * 获取教师在指定课程的所有学生本周学习情况
     * @param courseId 课程ID
     * @param weekStartDate 本周开始日期
     * @return 学生学习情况列表
     */
    List<Map<String, Object>> getCourseStudentsWeeklyTime(@Param("courseId") Long courseId,
                                                           @Param("weekStartDate") Date weekStartDate);

    /**
     * 获取教师所有课程的学生本周学习情况汇总
     * @param teacherId 教师ID
     * @param weekStartDate 本周开始日期
     * @return 课程学习情况列表
     */
    List<Map<String, Object>> getTeacherCoursesWeeklyTime(@Param("teacherId") Long teacherId,
                                                           @Param("weekStartDate") Date weekStartDate);

    /**
     * 获取学生最近几周的学习时间统计
     * @param studentId 学生ID
     * @param weeks 查询最近几周（默认4周）
     * @return 每周学习时间列表
     */
    List<Map<String, Object>> getStudentRecentWeeksTime(@Param("studentId") Long studentId,
                                                         @Param("weeks") Integer weeks);

    /**
     * 获取学生在指定课程最近几周的学习时间统计
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param weeks 查询最近几周（默认4周）
     * @return 每周学习时间列表
     */
    List<Map<String, Object>> getStudentCourseRecentWeeksTime(@Param("studentId") Long studentId,
                                                               @Param("courseId") Long courseId,
                                                               @Param("weeks") Integer weeks);

    /**
     * 清理过期的周记录（例如保留最近12周的数据）
     * @param beforeDate 在此日期之前的记录将被删除
     */
    void deleteBefore(@Param("beforeDate") Date beforeDate);

    List<WeeklyStudyTime> findByStudentAndCourse(@Param("studentId") Long studentId,
                                                  @Param("courseId") Long courseId);
}
