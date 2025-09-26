package com.ccut.service;

import com.ccut.entity.Student;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    int insert(Student student);
    Student getStudentByStudentNumber(String studentNumber);
    int update(Student student);
    int deleteById(Long id);
    List<Student> selectAll();
    Student selectById(Long id);
    List<Student> selectByGrade(String grade);
    List<Student> selectByClassName(String className);
}
