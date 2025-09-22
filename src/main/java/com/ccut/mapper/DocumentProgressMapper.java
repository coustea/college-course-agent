package com.ccut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DocumentProgressMapper {

    int upsert(@Param("studentId") Long studentId,
               @Param("courseId") Long courseId,
               @Param("documentId") Long documentId,
               @Param("deltaSec") Integer deltaSec,
               @Param("scrollPct") Double scrollPct,
               @Param("completed") Boolean completed);

    com.ccut.entity.DocumentProgress findOne(@Param("studentId") Long studentId,
                                             @Param("courseId") Long courseId,
                                             @Param("documentId") Long documentId);

    java.util.List<com.ccut.entity.DocumentProgress> listByCourse(@Param("studentId") Long studentId,
                                                                  @Param("courseId") Long courseId);

    int countCompletedDocumentsByCourse(@Param("studentId") Long studentId,
                                        @Param("courseId") Long courseId);

    int countDocumentsByCourse(@Param("courseId") Long courseId);
}


