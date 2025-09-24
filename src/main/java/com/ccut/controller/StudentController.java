package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.service.Impl.StudentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentServiceImpl studentService;

    @GetMapping("/by-grade")
    public Result<java.util.List<Student>> listByGrade(@RequestParam("grade") String grade){
        try {
            if (grade == null || grade.trim().isEmpty()) {
                return Result.error(400, "grade 不能为空");
            }
            return Result.success(studentService.selectByGrade(grade.trim()));
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}


