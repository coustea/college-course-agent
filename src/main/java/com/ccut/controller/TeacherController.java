package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.dto.TeacherCourseCard;
import com.ccut.dto.TeacherVideoItem;
import com.ccut.entity.Student;
import com.ccut.entity.Teacher;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.EnrollmentMapper;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.Impl.TeacherServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/teacher")
public class
TeacherController {
    @Autowired
    private StudentServiceImpl studentService;
    @Autowired
    private TeacherServiceImpl teacherService;
    @Autowired
    private EnrollmentMapper enrollmentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseVideoMapper courseVideoMapper;
    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    // 教师获取自己的班级
    @GetMapping("/classNames")
    public Result<List<String>> getClassNameByTeacherId(@RequestParam("teacherId") Long teacherId){
        try {
            List<String> classNames = teacherService.selectClassNameByTeacherId(teacherId);
            return Result.success(classNames);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Teacher> getById(@PathVariable Long id){
        try {
            Teacher teacher = teacherService.selectById(id);
            if (teacher != null) return Result.success(teacher);
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/insert/teacher")
    public Result<Teacher> insertTeacher(@RequestBody Teacher teacher){
        try {
            int n = teacherService.insert(teacher);
            if (n > 0) return Result.success(teacher);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping("/update/teacher/{id}")
    public Result<String> updateTeacher(@PathVariable("id") Long id, @RequestBody Teacher teacher){
        try {
            teacher.setId(id);
            log.debug("Updating teacher bio: {}", teacher.getBio());
            int n = teacherService.update(teacher);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete/teacher")
    public Result<String> deleteTeacher(@RequestParam("id") Long id){
        try {
            int n = teacherService.deleteById(id);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list/students")
    public Result<List<Student>> listStudents(){
        try {

            return Result.success(studentService.selectAll());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping("/update/student/{id}")
    public Result<String> updateStudent(@PathVariable("id") Long id, @RequestBody Student student) {
        try {
            if (id == null) return Result.error(400, "id 不能为空");
            student.setId(id);
            log.debug("Updating student: {}", student);
            int n = studentService.updateById(student);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list/teachers")
    public Result<List<Teacher>> listTeachers(){
        try {
            return Result.success(teacherService.selectAll());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 将学生加入课程（选课表 enrollments 关联）
    @PostMapping("/enroll")
    public Result<String> enroll(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId){
        try {
            int n = enrollmentMapper.upsert(studentId, courseId);
            if (n > 0) return Result.success("选课成功");
            return Result.error(500, "选课失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 按学生ID查询已加入的课程
    @GetMapping("/enrollments/courses")
    public Result<List<com.ccut.entity.Course>> findCoursesByStudentId(@RequestParam("studentId") Long studentId){
        try {
            List<com.ccut.entity.Course> courses = enrollmentMapper.findCoursesByStudentId(studentId);
            if (courses != null) {
                for (com.ccut.entity.Course c : courses) {
                    if (c != null && c.getCourseId() != null) {
                        c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                        c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                    }
                }
            }
            return Result.success(courses);
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 按课程ID查询参与课程的学生
    @GetMapping("/enrollments/students")
    public Result<List<Student>> findStudentsByCourseId(@RequestParam("courseId") Long courseId){
        try {
            return Result.success(enrollmentMapper.findStudentsByCourseId(courseId));
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }


    @DeleteMapping("/delete/student")
    public Result<String> deleteStudentById(@RequestParam("id") Long id){
        try {
            if (id == null) return Result.error(400, "id 不能为空");
            int n = studentService.deleteById(id);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到该学生");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 新增：按教师ID返回其课程下的视频列表，拍平字段以适配前端卡片需求
    @GetMapping("/videos")
    public Result<Map<String, Object>> listTeacherVideos(
            @RequestParam("teacherId") Long teacherId) {
        try {
            if (teacherId == null) return Result.error(400, "teacherId 不能为空");
            List<TeacherVideoItem> videos = courseVideoMapper.listByTeacherId(teacherId);
            List<TeacherCourseCard> courses = courseMapper.listCourseCardsByTeacher(teacherId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("videos", videos);
            resp.put("courses", courses);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
