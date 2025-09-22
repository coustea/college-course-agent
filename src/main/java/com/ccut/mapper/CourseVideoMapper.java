package com.ccut.mapper;

import com.ccut.entity.CourseVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseVideoMapper {
    int insert(CourseVideo video);
    int updateById(CourseVideo video);
    int deleteById(@Param("videoId") Long videoId);
    List<CourseVideo> findByCourseId(@Param("courseId") Long courseId);

    // 新增：按教师ID查询其课程下的视频列表
    java.util.List<com.ccut.entity.TeacherVideoItem> listByTeacherId(@Param("teacherId") Long teacherId);
}


