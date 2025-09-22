package com.ccut.service;

import com.ccut.entity.Teacher;

public interface TeacherService {
    int insert(Teacher teacher);
    int update(Teacher teacher);
    int deleteById(Long id);
    java.util.List<Teacher> selectAll();
    // 进度相关接口可放在独立 ProgressService，这里先不加
}
