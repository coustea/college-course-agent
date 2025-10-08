package com.ccut.mapper;

import com.ccut.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TeacherMapper {
    int insertTeacher(Teacher teacher);
    int updateById(Teacher teacher);
    int deleteById(Long id);
    List<Teacher> selectAll();
    Teacher selectById(Long id);

    // 教师获取自己的班级
    List<String> selectClassNameByTeacherId(Long teacherId);
}


