package com.ccut.mapper;

import com.ccut.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

    int insertStudent(Student student);
    Student getStudentByStudentNumber(String studentNumber);
    int updateById(Student student);
    int deleteById(Long id);
    List<Student> selectAll();
    Student selectById(Long id);
    List<Student> selectByGrade(String grade);

}
