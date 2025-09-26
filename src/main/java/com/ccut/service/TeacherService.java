package com.ccut.service;

import com.ccut.entity.Teacher;

import java.util.List;

public interface TeacherService {
    int insert(Teacher teacher);
    int update(Teacher teacher);
    int deleteById(Long id);
    List<Teacher> selectAll();



}
