package com.ccut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoProgressMapper {

    int upsert(@Param("studentId") Long studentId,
               @Param("courseId") Long courseId,
               @Param("videoId") Long videoId,
               @Param("deltaSec") Integer deltaSec,
               @Param("completed") Boolean completed);

    com.ccut.entity.VideoProgress findOne(@Param("studentId") Long studentId,
                                          @Param("courseId") Long courseId,
                                          @Param("videoId") Long videoId);

    java.util.List<com.ccut.entity.VideoProgress> listByCourse(@Param("studentId") Long studentId,
                                                               @Param("courseId") Long courseId);

    int countCompletedVideosByCourse(@Param("studentId") Long studentId,
                                     @Param("courseId") Long courseId);

    int countVideosByCourse(@Param("courseId") Long courseId);
}


