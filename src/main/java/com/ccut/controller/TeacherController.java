package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.entity.Teacher;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.TeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师控制器
 */
@Slf4j
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
        try {
            return Result.success(teacherService.selectClassNameByTeacherId(teacherId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 根据ID获取教师
     */
    @GetMapping("/{id}")
    public Result<Teacher> getById(@PathVariable Long id) {
        try {
            Teacher teacher = teacherService.selectById(id);
            if (teacher != null) {
                return Result.success(teacher);
            }
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 新增教师
     */
    @PostMapping("/insert/teacher")
    public Result<Teacher> insertTeacher(@RequestBody Teacher teacher) {
        try {
            int n = teacherService.insert(teacher);
            if (n > 0) {
                return Result.success(teacher);
            }
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新教师
     */
    @PutMapping("/update/teacher/{id}")
    public Result<String> updateTeacher(@PathVariable("id") Long id, @RequestBody Teacher teacher) {
        try {
            teacher.setId(id);
            log.debug("Updating teacher bio: {}", teacher.getBio());
            int n = teacherService.update(teacher);
            if (n > 0) {
                return Result.success("更新成功");
            }
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除教师
     */
    @DeleteMapping("/delete/teacher")
    public Result<String> deleteTeacher(@RequestParam("id") Long id) {
        try {
            int n = teacherService.deleteById(id);
            if (n > 0) {
                return Result.success("删除成功");
            }
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出所有学生
     */
    @GetMapping("/list/students")
    public Result<List<Student>> listStudents() {
        try {
            return Result.success(studentService.selectAll());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新学生
     */
    @PutMapping("/update/student/{id}")
    public Result<String> updateStudent(@PathVariable("id") Long id, @RequestBody Student student) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            student.setId(id);
            log.debug("Updating student: {}", student);
            int n = studentService.updateById(student);
            if (n > 0) {
                return Result.success("更新成功");
            }
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出所有教师
     */
    @GetMapping("/list/teachers")
    public Result<List<Teacher>> listTeachers() {
        try {
            return Result.success(teacherService.selectAll());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 学生选课
     */
    @PostMapping("/enroll")
    public Result<String> enroll(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId) {
        try {
            return Result.success(teacherService.enrollStudent(studentId, courseId));
        } catch (RuntimeException e) {
            log.warn("选课业务异常: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按学生ID查询已加入的课程
     */
    @GetMapping("/enrollments/courses")
    public Result<List<com.ccut.entity.Course>> findCoursesByStudentId(@RequestParam("studentId") Long studentId) {
        try {
            return Result.success(teacherService.findCoursesByStudentId(studentId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按课程ID查询参与课程的学生
     */
    @GetMapping("/enrollments/students")
    public Result<List<Student>> findStudentsByCourseId(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(teacherService.findStudentsByCourseId(courseId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/delete/student")
    public Result<String> deleteStudentById(@RequestParam("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            int n = studentService.deleteById(id);
            if (n > 0) {
                return Result.success("删除成功");
            }
            return Result.error(404, "未找到该学生");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按教师ID返回其课程下的视频列表
     */
    @GetMapping("/videos")
    public Result<Map<String, Object>> listTeacherVideos(
            @RequestParam("teacherId") Long teacherId) {
        try {
            return Result.success(teacherService.listTeacherVideos(teacherId));
        } catch (IllegalArgumentException e) {
            log.warn("查询教师视频参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

}
