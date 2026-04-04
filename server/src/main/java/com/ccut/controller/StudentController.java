package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.exception.BusinessException;
import com.ccut.service.Impl.StudentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentServiceImpl studentService;

    @GetMapping("/by-grade")
    public Result<List<Student>> listByGrade(@RequestParam("grade") String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            throw new IllegalArgumentException("grade 不能为空");
        }
        List<Student> students = studentService.selectByGrade(grade.trim());
        return Result.success(students);
    }

    @PostMapping("/by-id")
    public Result<Student> selectById(@RequestParam("userId") Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        Student student = studentService.selectById(userId);
        if (student == null) {
            throw new BusinessException(404, "未找到该学生");
        }
        return Result.success(student);
    }

    @PostMapping
    public Result<Student> insert(@RequestBody Student student) {
        int res = studentService.insert(student);
        if (res <= 0) {
            throw new RuntimeException("添加学生失败");
        }
        return Result.success(student);
    }

    @GetMapping("/class/{className}")
    public Result<List<Student>> selectByClassName(@PathVariable("className") String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("className 不能为空");
        }
        List<Student> students = studentService.selectByClassName(className.trim());
        return Result.success(students);
    }
}
