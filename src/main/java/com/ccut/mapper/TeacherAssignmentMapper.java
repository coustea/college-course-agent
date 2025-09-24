package com.ccut.mapper;


import com.ccut.entity.TeacherAssignment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TeacherAssignmentMapper {

    int insert(TeacherAssignment teacherAssignment);
    List<TeacherAssignment> selectByTeacherId(Long teacherId);
    int update(TeacherAssignment teacherAssignment);
    int delete(Long assignmentId);
}
