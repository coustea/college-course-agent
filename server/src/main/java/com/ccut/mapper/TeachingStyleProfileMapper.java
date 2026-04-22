package com.ccut.mapper;

import com.ccut.entity.TeachingStyleProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeachingStyleProfileMapper {
    int insert(TeachingStyleProfile profile);
    int updateById(TeachingStyleProfile profile);
    TeachingStyleProfile findByTeacherId(@Param("teacherId") Long teacherId);
    int upsert(TeachingStyleProfile profile);
}
