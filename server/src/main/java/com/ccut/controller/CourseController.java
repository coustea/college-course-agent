package com.ccut.controller;

import com.ccut.dto.CourseStatistics;
import com.ccut.dto.Result;
import com.ccut.entity.Course;
import com.ccut.service.CourseService;
import com.ccut.service.ProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程控制器
 */
@RestController
@RequestMapping("/api/course")
@Slf4j
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private ProgressService progressService;

    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Course> insert(
            @RequestParam(value = "course", required = false) String courseJson,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
        Course result = courseService.insertWithImage(course, image);
        return Result.success(result);
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam(value = "course", required = false) String courseJson,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
        course.setCourseId(courseId);
        String result = courseService.updateWithImage(course, image);
        return Result.success(result);
    }

    @PostMapping(value = "/insert", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Course> insertJson(@RequestBody Course course) {
        int n = courseService.insert(course);
        if (n > 0) {
            return Result.success(course);
        }
        throw new RuntimeException("添加失败");
    }

    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updateJson(@RequestBody Course course) {
        if (course.getCourseId() == null) {
            throw new IllegalArgumentException("courseId 不能为空");
        }
        int n = courseService.updateById(course);
        if (n > 0) {
            return Result.success("更新成功");
        }
        throw new RuntimeException("未找到或未变更");
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("courseId") Long courseId) {
        int n = courseService.deleteById(courseId);
        if (n > 0) {
            return Result.success("删除成功");
        }
        throw new RuntimeException("未找到");
    }

    @GetMapping("/list")
    public Result<java.util.List<Course>> list() {
        List<Course> courses = courseService.selectAll();
        return Result.success(courses);
    }

    @GetMapping("/detail")
    public Result<Course> detail(@RequestParam("courseId") Long courseId) {
        Course course = courseService.selectById(courseId);
        return Result.success(course);
    }

    @GetMapping("/search")
    public Result<java.util.List<Course>> search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {
        List<Course> courses = courseService.search(name, description);
        return Result.success(courses);
    }

    @GetMapping("/stats/all")
    public Result<List<CourseStatistics>> getAllCourseStatistics(@RequestParam("teacherId") Long teacherId) {
        List<CourseStatistics> statistics = progressService.getAllCourseStatistics(teacherId);
        return Result.success(statistics);
    }

    @PostMapping("/{courseId}/publish")
    public Result<String> publishCourse(
            @PathVariable("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId) {
        boolean success = courseService.publishCourse(courseId, teacherId);
        if (success) {
            return Result.success("课程发布成功");
        }
        throw new RuntimeException("课程发布失败");
    }

    @PostMapping("/{courseId}/unpublish")
    public Result<String> unpublishCourse(
            @PathVariable("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId) {
        boolean success = courseService.unpublishCourse(courseId, teacherId);
        if (success) {
            return Result.success("课程已取消发布");
        }
        throw new RuntimeException("取消发布失败");
    }

    @GetMapping("/teacher/{teacherId}/all")
    public Result<List<Course>> getCoursesByTeacherId(@PathVariable("teacherId") Long teacherId) {
        List<Course> courses = courseService.getCoursesByTeacherId(teacherId);
        return Result.success(courses);
    }

    @GetMapping("/published")
    public Result<List<Course>> getPublishedCourses() {
        List<Course> courses = courseService.getPublishedCourses();
        return Result.success(courses);
    }

    /**
     * 解析课程请求参数（Controller 层的参数处理职责）
     */
    private Course parseCourseFromRequest(String courseJson, String courseCode, String courseName,
                                          String description, Long teacherId) {
        Course course;
        if (courseJson != null && !courseJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                course = mapper.readValue(courseJson, Course.class);
            } catch (Exception e) {
                throw new RuntimeException("课程 JSON 解析失败：" + e.getMessage(), e);
            }
        } else {
            course = new Course();
            course.setCourseCode(courseCode);
            course.setCourseName(courseName);
            course.setDescription(description);
            course.setTeacherId(teacherId);
        }
        return course;
    }
}
