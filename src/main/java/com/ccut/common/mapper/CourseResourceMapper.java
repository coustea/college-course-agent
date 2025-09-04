package com.ccut.common.mapper;


import com.ccut.common.entity.CourseResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseResourceMapper {

    int insert(CourseResource courseResource);

    int deleteById(@Param("id") Integer id);

    int updateById(CourseResource courseResource);

    List<CourseResource> selectById(Integer id);
}
