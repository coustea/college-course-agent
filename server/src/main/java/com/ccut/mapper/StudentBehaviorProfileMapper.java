package com.ccut.mapper;

import com.ccut.entity.StudentBehaviorProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学生行为画像Mapper接口
 */
@Mapper
public interface StudentBehaviorProfileMapper {

    /**
     * 插入画像记录
     */
    int insert(StudentBehaviorProfile profile);

    /**
     * 更新画像记录
     */
    int updateById(StudentBehaviorProfile profile);

    /**
     * 根据ID查询
     */
    StudentBehaviorProfile selectById(Long profileId);

    /**
     * 根据学生ID查询
     */
    StudentBehaviorProfile selectByStudentId(Long studentId);

    /**
     * 根据学生ID删除
     */
    int deleteByStudentId(Long studentId);

    /**
     * 更新或插入（存在则更新，不存在则插入）
     */
    int upsert(StudentBehaviorProfile profile);

    /**
     * 批量更新画像
     */
    int batchUpdate(@Param("list") java.util.List<StudentBehaviorProfile> list);
}