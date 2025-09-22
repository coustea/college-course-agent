package com.ccut.mapper;

import com.ccut.entity.StudentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentGroupMapper {
    int insert(StudentGroup g);
    int updateById(StudentGroup g);
    int deleteById(@Param("id") Long id);
    StudentGroup selectById(@Param("id") Long id);
    List<StudentGroup> listByCourseId(@Param("courseId") Long courseId);
    List<StudentGroup> listByStudentId(@Param("studentId") Long studentId);
}


