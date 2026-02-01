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

    /**
     * 插入课程（带图片上传）
     */
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Course> insert(
            @RequestParam(value = "course", required = false) String courseJson,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
            return Result.success(courseService.insertWithImage(course, image));
        } catch (Exception e) {
            log.error("课程上传失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新课程（带图片上传）
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam(value = "course", required = false) String courseJson,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
            course.setCourseId(courseId);
            return Result.success(courseService.updateWithImage(course, image));
        } catch (IllegalArgumentException e) {
            log.warn("更新课程参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新课程业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("课程更新失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 兼容：纯 JSON 插入（不上传图片）
     */
    @PostMapping(value = "/insert", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Course> insertJson(@RequestBody Course course) {
        try {
            int n = courseService.insert(course);
            if (n > 0) {
                return Result.success(course);
            }
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 兼容：纯 JSON 更新（不上传图片）
     */
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updateJson(@RequestBody Course course) {
        try {
            if (course.getCourseId() == null) {
                return Result.error(400, "courseId 不能为空");
            }
            int n = courseService.updateById(course);
            if (n > 0) {
                return Result.success("更新成功");
            }
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("courseId") Long courseId) {
        try {
            int n = courseService.deleteById(courseId);
            if (n > 0) {
                return Result.success("删除成功");
            }
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询所有课程
     */
    @GetMapping("/list")
    public Result<java.util.List<Course>> list() {
        try {
            return Result.success(courseService.selectAll());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询课程详情
     */
    @GetMapping("/detail")
    public Result<Course> detail(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(courseService.selectById(courseId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 搜索课程
     */
    @GetMapping("/search")
    public Result<java.util.List<Course>> search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {
        try {
            return Result.success(courseService.search(name, description));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取指定教师的所有已发布课程的统计数据（包含完成率）
     * 用于教师端Home页面展示课程统计信息
     * 只统计选课了该课程的学生数据
     * 直接调用SQL查询，高效聚合数据
     */
    @GetMapping("/stats/all")
    public Result<List<CourseStatistics>> getAllCourseStatistics(@RequestParam("teacherId") Long teacherId) {
        try {
            List<CourseStatistics> statistics = progressService.getAllCourseStatistics(teacherId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取课程统计数据失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 发布课程
     */
    @PostMapping("/{courseId}/publish")
    public Result<String> publishCourse(
            @PathVariable("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId) {
        try {
            boolean success = courseService.publishCourse(courseId, teacherId);
            if (success) {
                return Result.success("课程发布成功");
            }
            return Result.error(500, "课程发布失败");
        } catch (IllegalArgumentException e) {
            log.warn("发布课程参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("发布课程业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("课程发布失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 取消发布课程
     */
    @PostMapping("/{courseId}/unpublish")
    public Result<String> unpublishCourse(
            @PathVariable("courseId") Long courseId,
            @RequestParam("teacherId") Long teacherId) {
        try {
            boolean success = courseService.unpublishCourse(courseId, teacherId);
            if (success) {
                return Result.success("课程已取消发布");
            }
            return Result.error(500, "取消发布失败");
        } catch (IllegalArgumentException e) {
            log.warn("取消发布课程参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("取消发布课程业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("取消发布课程失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询教师的所有课程（包含草稿和已发布）
     */
    @GetMapping("/teacher/{teacherId}/all")
    public Result<List<Course>> getCoursesByTeacherId(@PathVariable("teacherId") Long teacherId) {
        try {
            return Result.success(courseService.getCoursesByTeacherId(teacherId));
        } catch (Exception e) {
            log.error("获取教师课程列表失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询所有已发布的课程列表（学生端使用）
     */
    @GetMapping("/published")
    public Result<List<Course>> getPublishedCourses() {
        try {
            return Result.success(courseService.getPublishedCourses());
        } catch (Exception e) {
            log.error("获取已发布课程列表失败: {}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 解析课程请求参数（Controller层的参数处理职责）
     */
    private Course parseCourseFromRequest(String courseJson, String courseCode, String courseName,
                                          String description, Long teacherId) throws Exception {
        Course course;
        if (courseJson != null && !courseJson.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            course = mapper.readValue(courseJson, Course.class);
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
