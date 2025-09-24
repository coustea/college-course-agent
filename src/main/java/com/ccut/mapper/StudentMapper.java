package com.ccut.mapper;

import com.ccut.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper {

    int insertStudent(Student student);
    Student getStudentByStudentNumber(String studentNumber);
    int updateById(Student student);
    int deleteById(Long id);
    java.util.List<Student> selectAll();
    Student selectById(Long id);
    java.util.List<Student> selectByGrade(String grade);
}
