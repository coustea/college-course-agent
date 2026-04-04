package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.entity.Teacher;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师控制器
 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private StudentServiceImpl studentService;

    @Autowired
    private TeacherService teacherService;

    /**
     * 教师获取自己的班级
     */
    @GetMapping("/classNames")
    public Result<List<String>> getClassNameByTeacherId(@RequestParam("teacherId") Long teacherId) {
        List<String> classNames = teacherService.selectClassNameByTeacherId(teacherId);
        return Result.success(classNames);
    }

    /**
     * 根据 ID 获取教师
     */
    @GetMapping("/{id}")
    public Result<Teacher> getById(@PathVariable Long id) {
        Teacher teacher = teacherService.selectById(id);
        if (teacher != null) {
            return Result.success(teacher);
        }
        throw new RuntimeException("未找到");
    }

    /**
     * 新增教师
     */
    @PostMapping("/insert/teacher")
    public Result<Teacher> insertTeacher(@RequestBody Teacher teacher) {
        int n = teacherService.insert(teacher);
        if (n > 0) {
            return Result.success(teacher);
        }
        throw new RuntimeException("添加失败");
    }

    /**
     * 更新教师
     */
    @PutMapping("/update/teacher/{id}")
    public Result<String> updateTeacher(@PathVariable("id") Long id, @RequestBody Teacher teacher) {
        teacher.setId(id);
        int n = teacherService.update(teacher);
        if (n > 0) {
            return Result.success("更新成功");
        }
        throw new RuntimeException("未找到或未变更");
    }

    /**
     * 删除教师
     */
    @DeleteMapping("/delete/teacher")
    public Result<String> deleteTeacher(@RequestParam("id") Long id) {
        int n = teacherService.deleteById(id);
        if (n > 0) {
            return Result.success("删除成功");
        }
        throw new RuntimeException("未找到");
    }

    /**
     * 列出所有学生
     */
    @GetMapping("/list/students")
    public Result<List<Student>> listStudents() {
        List<Student> students = studentService.selectAll();
        return Result.success(students);
    }

    /**
     * 更新学生
     */
    @PutMapping("/update/student/{id}")
    public Result<String> updateStudent(@PathVariable("id") Long id, @RequestBody Student student) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        student.setId(id);
        int n = studentService.updateById(student);
        if (n > 0) {
            return Result.success("更新成功");
        }
        throw new RuntimeException("未找到或未变更");
    }

    /**
     * 列出所有教师
     */
    @GetMapping("/list/teachers")
    public Result<List<Teacher>> listTeachers() {
        List<Teacher> teachers = teacherService.selectAll();
        return Result.success(teachers);
    }

    /**
     * 学生选课
     */
    @PostMapping("/enroll")
    public Result<String> enroll(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId) {
        String result = teacherService.enrollStudent(studentId, courseId);
        return Result.success(result);
    }

    /**
     * 按学生 ID 查询已加入的课程
     */
    @GetMapping("/enrollments/courses")
    public Result<List<com.ccut.entity.Course>> findCoursesByStudentId(@RequestParam("studentId") Long studentId) {
        List<com.ccut.entity.Course> courses = teacherService.findCoursesByStudentId(studentId);
        return Result.success(courses);
    }

    /**
     * 按课程 ID 查询参与课程的学生
     */
    @GetMapping("/enrollments/students")
    public Result<List<Student>> findStudentsByCourseId(@RequestParam("courseId") Long courseId) {
        List<Student> students = teacherService.findStudentsByCourseId(courseId);
        return Result.success(students);
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/delete/student")
    public Result<String> deleteStudentById(@RequestParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        int n = studentService.deleteById(id);
        if (n > 0) {
            return Result.success("删除成功");
        }
        throw new RuntimeException("未找到该学生");
    }

    /**
     * 按教师 ID 返回其课程下的视频列表
     */
    @GetMapping("/videos")
    public Result<Map<String, Object>> listTeacherVideos(
            @RequestParam("teacherId") Long teacherId) {
        Map<String, Object> result = teacherService.listTeacherVideos(teacherId);
        return Result.success(result);
    }
}
