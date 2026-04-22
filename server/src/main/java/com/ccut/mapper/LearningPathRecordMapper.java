package com.ccut.mapper;

import com.ccut.entity.LearningPathRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习路径记录Mapper接口
 */
@Mapper
public interface LearningPathRecordMapper {

    /**
     * 插入记录
     */
    int insert(LearningPathRecord record);

    /**
     * 根据ID查询
     */
    LearningPathRecord selectById(Long recordId);

    /**
     * 查询学生学习路径
     */
    List<LearningPathRecord> selectByStudentId(@Param("studentId") Long studentId,
                                                @Param("courseId") Long courseId,
                                                @Param("limit") Integer limit);

    /**
     * 查询学生最近学习记录
     */
    List<LearningPathRecord> selectRecentByStudentId(@Param("studentId") Long studentId,
                                                      @Param("limit") Integer limit);

    /**
     * 查询课程学习路径统计
     */
    List<LearningPathRecord> selectByCourseId(@Param("courseId") Long courseId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 统计学生资源访问次数
     */
    int countByStudentAndResource(@Param("studentId") Long studentId,
                                   @Param("resourceType") String resourceType,
                                   @Param("startTime") LocalDateTime startTime);

    /**
     * 统计学生总学习时长
     */
    Integer sumDurationByStudent(@Param("studentId") Long studentId,
                                  @Param("courseId") Long courseId,
                                  @Param("startTime") LocalDateTime startTime);

    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<LearningPathRecord> list);

    /**
     * 删除学生旧记录（数据清理）
     */
    int deleteOldRecords(@Param("beforeTime") LocalDateTime beforeTime);
}