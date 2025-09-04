package com.ccut.common.mapper;

import com.ccut.common.entity.Teacher;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TeacherMapper {

    @Select("select * from `teacher` where username = #{username}")
    Teacher selectByUsername(String username);

    int insert(Teacher teacher);

    @Delete("delete  from `teacher` where id = #{id}")
    int delete(Integer id);

    int updateById(Teacher teacher);

    List<Teacher> selectAll(Teacher teacher);

    @Select("select * from `teacher` where id = #{id}")
    Teacher selectById(Integer id);

    List<Teacher> findByCourseId(Integer id);
}
