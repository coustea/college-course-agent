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
        log.debug("收到查询教师班级请求：URI=/api/teacher/classNames, 参数：teacherId={}", teacherId);
        try {
            log.info("执行查询教师班级业务：teacherId={}", teacherId);
            List<String> classNames = teacherService.selectClassNameByTeacherId(teacherId);
            log.debug("查询教师班级成功：teacherId={}, 班级数={}", teacherId, classNames.size());
            return Result.success(classNames);
        } catch (Exception e) {
            log.error("查询教师班级异常：teacherId={}, 错误：{}", teacherId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 根据 ID 获取教师
     */
    @GetMapping("/{id}")
    public Result<Teacher> getById(@PathVariable Long id) {
        log.debug("收到查询教师详情请求：URI=/api/teacher/{}, 参数：id={}", id, id);
        try {
            log.info("执行查询教师详情业务：id={}", id);
            Teacher teacher = teacherService.selectById(id);
            if (teacher != null) {
                log.debug("查询教师成功：id={}, name={}", id, teacher.getName());
                return Result.success(teacher);
            }
            log.warn("未找到教师：id={}", id);
            return Result.error(404, "未找到");
        } catch (Exception e) {
            log.error("查询教师异常：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 新增教师
     */
    @PostMapping("/insert/teacher")
    public Result<Teacher> insertTeacher(@RequestBody Teacher teacher) {
        log.debug("收到插入教师请求：URI=/api/teacher/insert/teacher, 参数：teacher={}", teacher);
        try {
            log.info("执行插入教师业务：name={}", teacher.getName());
            int n = teacherService.insert(teacher);
            if (n > 0) {
                log.debug("插入教师成功：id={}, name={}", teacher.getId(), teacher.getName());
                return Result.success(teacher);
            }
            log.warn("插入教师失败：teacher={}", teacher);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            log.error("插入教师异常：teacher={}, 错误：{}", teacher, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新教师
     */
    @PutMapping("/update/teacher/{id}")
    public Result<String> updateTeacher(@PathVariable("id") Long id, @RequestBody Teacher teacher) {
        log.debug("收到更新教师请求：URI=/api/teacher/update/teacher/{}, 参数：id={}, bio={}", id, teacher.getBio());
        try {
            teacher.setId(id);
            log.info("执行更新教师业务：id={}", id);
            int n = teacherService.update(teacher);
            if (n > 0) {
                log.debug("更新教师成功：id={}", id);
                return Result.success("更新成功");
            }
            log.warn("更新教师失败：id={}, teacher={}", id, teacher);
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            log.error("更新教师异常：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除教师
     */
    @DeleteMapping("/delete/teacher")
    public Result<String> deleteTeacher(@RequestParam("id") Long id) {
        log.debug("收到删除教师请求：URI=/api/teacher/delete/teacher, 参数：id={}", id);
        try {
            log.info("执行删除教师业务：id={}", id);
            int n = teacherService.deleteById(id);
            if (n > 0) {
                log.debug("删除教师成功：id={}", id);
                return Result.success("删除成功");
            }
            log.warn("未找到教师：id={}", id);
            return Result.error(404, "未找到");
        } catch (Exception e) {
            log.error("删除教师异常：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出所有学生
     */
    @GetMapping("/list/students")
    public Result<List<Student>> listStudents() {
        log.debug("收到查询所有学生请求：URI=/api/teacher/list/students");
        try {
            log.info("执行查询所有学生业务");
            List<Student> students = studentService.selectAll();
            log.debug("查询所有学生成功：结果数={}", students.size());
            return Result.success(students);
        } catch (Exception e) {
            log.error("查询所有学生异常：错误：{}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新学生
     */
    @PutMapping("/update/student/{id}")
    public Result<String> updateStudent(@PathVariable("id") Long id, @RequestBody Student student) {
        log.debug("收到更新学生请求：URI=/api/teacher/update/student/{}, 参数：id={}, student={}", id, student);
        try {
            if (id == null) {
                log.warn("更新学生参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            student.setId(id);
            log.info("执行更新学生业务：id={}", id);
            int n = studentService.updateById(student);
            if (n > 0) {
                log.debug("更新学生成功：id={}", id);
                return Result.success("更新成功");
            }
            log.warn("更新学生失败：id={}, student={}", id, student);
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            log.error("更新学生异常：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 列出所有教师
     */
    @GetMapping("/list/teachers")
    public Result<List<Teacher>> listTeachers() {
        log.debug("收到查询所有教师请求：URI=/api/teacher/list/teachers");
        try {
            log.info("执行查询所有教师业务");
            List<Teacher> teachers = teacherService.selectAll();
            log.debug("查询所有教师成功：结果数={}", teachers.size());
            return Result.success(teachers);
        } catch (Exception e) {
            log.error("查询所有教师异常：错误：{}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 学生选课
     */
    @PostMapping("/enroll")
    public Result<String> enroll(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId) {
        log.debug("收到学生选课请求：URI=/api/teacher/enroll, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            log.info("执行学生选课业务：studentId={}, courseId={}", studentId, courseId);
            String result = teacherService.enrollStudent(studentId, courseId);
            log.debug("学生选课成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("选课业务异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage());
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("学生选课异常：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按学生 ID 查询已加入的课程
     */
    @GetMapping("/enrollments/courses")
    public Result<List<com.ccut.entity.Course>> findCoursesByStudentId(@RequestParam("studentId") Long studentId) {
        log.debug("收到查询学生课程请求：URI=/api/teacher/enrollments/courses, 参数：studentId={}", studentId);
        try {
            log.info("执行查询学生课程业务：studentId={}", studentId);
            List<com.ccut.entity.Course> courses = teacherService.findCoursesByStudentId(studentId);
            log.debug("查询学生课程成功：studentId={}, 课程数={}", studentId, courses.size());
            return Result.success(courses);
        } catch (Exception e) {
            log.error("查询学生课程异常：studentId={}, 错误：{}", studentId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按课程 ID 查询参与课程的学生
     */
    @GetMapping("/enrollments/students")
    public Result<List<Student>> findStudentsByCourseId(@RequestParam("courseId") Long courseId) {
        log.debug("收到查询课程学生请求：URI=/api/teacher/enrollments/students, 参数：courseId={}", courseId);
        try {
            log.info("执行查询课程学生业务：courseId={}", courseId);
            List<Student> students = teacherService.findStudentsByCourseId(courseId);
            log.debug("查询课程学生成功：courseId={}, 学生数={}", courseId, students.size());
            return Result.success(students);
        } catch (Exception e) {
            log.error("查询课程学生异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/delete/student")
    public Result<String> deleteStudentById(@RequestParam("id") Long id) {
        log.debug("收到删除学生请求：URI=/api/teacher/delete/student, 参数：id={}", id);
        try {
            if (id == null) {
                log.warn("删除学生参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            log.info("执行删除学生业务：id={}", id);
            int n = studentService.deleteById(id);
            if (n > 0) {
                log.debug("删除学生成功：id={}", id);
                return Result.success("删除成功");
            }
            log.warn("未找到学生：id={}", id);
            return Result.error(404, "未找到该学生");
        } catch (Exception e) {
            log.error("删除学生异常：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按教师 ID 返回其课程下的视频列表
     */
    @GetMapping("/videos")
    public Result<Map<String, Object>> listTeacherVideos(
            @RequestParam("teacherId") Long teacherId) {
        log.debug("收到查询教师视频请求：URI=/api/teacher/videos, 参数：teacherId={}", teacherId);
        try {
            log.info("执行查询教师视频业务：teacherId={}", teacherId);
            Map<String, Object> result = teacherService.listTeacherVideos(teacherId);
            log.debug("查询教师视频成功：teacherId={}", teacherId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("查询教师视频参数错误：teacherId={}, 错误：{}", teacherId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询教师视频异常：teacherId={}, 错误：{}", teacherId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
