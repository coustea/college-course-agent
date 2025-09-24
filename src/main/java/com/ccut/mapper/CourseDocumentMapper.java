package com.ccut.mapper;

import com.ccut.entity.CourseDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseDocumentMapper {
    int insert(CourseDocument document);
    int updateById(CourseDocument document);
    int deleteById(@Param("documentId") Long documentId);
    List<CourseDocument> findByCourseId(@Param("courseId") Long courseId);
    Integer findMaxIndexByCourseId(@Param("courseId") Long courseId);
}


