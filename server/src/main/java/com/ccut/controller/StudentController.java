package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.service.Impl.StudentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@Slf4j
public class StudentController {

    @Autowired
    private StudentServiceImpl studentService;

    @GetMapping("/by-grade")
    public Result<List<Student>> listByGrade(@RequestParam("grade") String grade) {
        log.debug("收到按年级查询学生请求：URI=/api/student/by-grade, 参数：grade={}", grade);
        try {
            if (grade == null || grade.trim().isEmpty()) {
                log.warn("按年级查询学生参数错误：grade 为空");
                return Result.error(400, "grade 不能为空");
            }
            log.info("执行按年级查询学生业务：grade={}", grade.trim());
            List<Student> students = studentService.selectByGrade(grade.trim());
            log.debug("按年级查询学生成功：grade={}, 结果数={}", grade, students.size());
            return Result.success(students);
        } catch (Exception e) {
            log.error("按年级查询学生异常：grade={}, 错误：{}", grade, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/by-id")
    public Result<Student> selectById(@RequestParam("userId") Long userId) {
        log.debug("收到按 ID 查询学生请求：URI=/api/student/by-id, 参数：userId={}", userId);
        try {
            if (userId == null) {
                log.warn("按 ID 查询学生参数错误：userId 为空");
                return Result.error(400, "userId 不能为空");
            }
            log.info("执行按 ID 查询学生业务：userId={}", userId);
            Student student = studentService.selectById(userId);
            if (student == null) {
                log.warn("未找到学生：userId={}", userId);
                return Result.error(404, "未找到该学生");
            }
            log.debug("按 ID 查询学生成功：userId={}, name={}", userId, student.getName());
            return Result.success(student);
        } catch (Exception e) {
            log.error("按 ID 查询学生异常：userId={}, 错误：{}", userId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping
    public Result<Student> insert(@RequestBody Student student) {
        log.debug("收到插入学生请求：URI=/api/student, 参数：student={}", student);
        try {
            log.info("执行插入学生业务：studentNumber={}, name={}", student.getStudentNumber(), student.getName());
            int res = studentService.insert(student);
            if (res <= 0) {
                log.error("添加学生失败：student={}", student);
                return Result.error(500, "添加学生失败");
            }
            log.debug("添加学生成功：studentNumber={}, id={}", student.getStudentNumber(), student.getId());
            return Result.success(student);
        } catch (Exception e) {
            log.error("添加学生异常：student={}, 错误：{}", student, e.getMessage(), e);
            return Result.error(500, "添加失败：" + e.getMessage());
        }
    }

    @GetMapping("/class/{className}")
    public Result<List<Student>> selectByClassName(@PathVariable("className") String className) {
        log.debug("收到按班级查询学生请求：URI=/api/student/class/{}, 参数：className={}", className, className);
        try {
            if (className == null || className.trim().isEmpty()) {
                log.warn("按班级查询学生参数错误：className 为空");
                return Result.error(400, "className 不能为空");
            }
            log.info("执行按班级查询学生业务：className={}", className.trim());
            List<Student> students = studentService.selectByClassName(className.trim());
            log.debug("按班级查询学生成功：className={}, 结果数={}", className, students.size());
            return Result.success(students);
        } catch (Exception e) {
            log.error("按班级查询学生异常：className={}, 错误：{}", className, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
