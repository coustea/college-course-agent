package com.ccut.mapper;

import com.ccut.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherMapper {
    int insertTeacher(Teacher teacher);
    int updateById(Teacher teacher);
    int deleteById(Long id);
    java.util.List<Teacher> selectAll();
    Teacher selectById(Long id);
}


