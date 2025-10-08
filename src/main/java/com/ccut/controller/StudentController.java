package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.entity.User;
import com.ccut.service.Impl.StudentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@Slf4j
public class StudentController {

    @Autowired
    private StudentServiceImpl studentService;

    @GetMapping("/by-grade")
    public Result<java.util.List<Student>> listByGrade(@RequestParam("grade") String grade) {
        try {
            if (grade == null || grade.trim().isEmpty()) {
                return Result.error(400, "grade 不能为空");
            }
            return Result.success(studentService.selectByGrade(grade.trim()));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/by-id")
    public Result<Student> selectById(@RequestParam("userId") Long userId) {
        try {
            if (userId == null) {
                return Result.error(400, "userId 不能为空");
            }
            Student student = studentService.selectById(userId);
            if (student == null) {
                return Result.error(404, "未找到该学生");
            }
            return Result.success(student);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping
    public Result<Student> insert(@RequestBody Student student) {
        int res = studentService.insert(student);
        if (res <= 0) {
            log.error("添加学生失败：{}", student);
            return Result.error(500, "添加学生失败");
        }
        return Result.success(student);
    }

    @GetMapping("/class/{className}")
    public Result<List<Student>> selectByClassName(@PathVariable("className") String className) {
        try {
            if (className == null || className.trim().isEmpty()) {
                return Result.error(400, "className 不能为空");
            }
            return Result.success(studentService.selectByClassName(className.trim()));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }


}


