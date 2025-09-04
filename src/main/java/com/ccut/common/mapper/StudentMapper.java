package com.ccut.common.mapper;

import com.ccut.common.entity.Student;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudentMapper {

    int insert(Student student);

    @Delete("DELETE FROM `student` WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    int updateById(Student student);

    @Select("select * from `student` where username = #{username}")
    Student selectByUsername(String username);

    @Select("select * from `student` where id = #{id}")
    Student selectById(Integer id);

    List<Student> selectAll(Student student);

}
