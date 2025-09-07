package com.ccut.service;

import com.ccut.entity.Student;

public interface StudentService {

    int insert(Student student);
    Student getStudentByStudentNumber(String studentNumber);
    int update(Student student);

}
