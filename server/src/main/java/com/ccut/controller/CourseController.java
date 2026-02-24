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
        log.debug("收到插入课程请求：URI=/api/course/insert, 参数：courseCode={}, courseName={}, teacherId={}, 有图片={}",
                courseCode, courseName, teacherId, image != null && !image.isEmpty());
        try {
            Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
            log.info("执行插入课程业务：courseCode={}, courseName={}", courseCode, courseName);
            Course result = courseService.insertWithImage(course, image);
            log.debug("插入课程成功：courseId={}, courseName={}", result.getCourseId(), result.getCourseName());
            return Result.success(result);
        } catch (Exception e) {
            log.error("课程上传失败：courseCode={}, courseName={}, 错误：{}", courseCode, courseName, e.getMessage(), e);
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
        log.debug("收到更新课程请求：URI=/api/course/update, 参数：courseId={}, courseCode={}, 有图片={}",
                courseId, courseCode, image != null && !image.isEmpty());
        try {
            Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
            course.setCourseId(courseId);
            log.info("执行更新课程业务：courseId={}", courseId);
            String result = courseService.updateWithImage(course, image);
            log.debug("更新课程成功：courseId={}", courseId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("更新课程参数错误：courseId={}, 错误：{}", courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新课程业务异常：courseId={}, 错误：{}", courseId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("课程更新失败：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 兼容：纯 JSON 插入（不上传图片）
     */
    @PostMapping(value = "/insert", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Course> insertJson(@RequestBody Course course) {
        log.debug("收到插入课程请求 (JSON)：URI=/api/course/insert, 参数：course={}", course);
        try {
            log.info("执行插入课程业务 (JSON)：courseCode={}, courseName={}", course.getCourseCode(), course.getCourseName());
            int n = courseService.insert(course);
            if (n > 0) {
                log.debug("插入课程成功 (JSON)：courseId={}", course.getCourseId());
                return Result.success(course);
            }
            log.warn("插入课程失败 (JSON)：course={}", course);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            log.error("插入课程异常 (JSON)：course={}, 错误：{}", course, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 兼容：纯 JSON 更新（不上传图片）
     */
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updateJson(@RequestBody Course course) {
        log.debug("收到更新课程请求 (JSON)：URI=/api/course/update, 参数：course={}", course);
        try {
            if (course.getCourseId() == null) {
                log.warn("更新课程参数错误 (JSON)：courseId 为空");
                return Result.error(400, "courseId 不能为空");
            }
            log.info("执行更新课程业务 (JSON)：courseId={}", course.getCourseId());
            int n = courseService.updateById(course);
            if (n > 0) {
                log.debug("更新课程成功 (JSON)：courseId={}", course.getCourseId());
                return Result.success("更新成功");
            }
            log.warn("更新课程失败 (JSON)：courseId={}", course.getCourseId());
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            log.error("更新课程异常 (JSON)：course={}, 错误：{}", course, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("courseId") Long courseId) {
        log.debug("收到删除课程请求：URI=/api/course/delete, 参数：courseId={}", courseId);
        try {
            log.info("执行删除课程业务：courseId={}", courseId);
            int n = courseService.deleteById(courseId);
            if (n > 0) {
                log.debug("删除课程成功：courseId={}", courseId);
                return Result.success("删除成功");
            }
            log.warn("未找到课程：courseId={}", courseId);
            return Result.error(404, "未找到");
        } catch (Exception e) {
            log.error("删除课程异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询所有课程
     */
    @GetMapping("/list")
    public Result<java.util.List<Course>> list() {
        log.debug("收到查询所有课程请求：URI=/api/course/list");
        try {
            log.info("执行查询所有课程业务");
            List<Course> courses = courseService.selectAll();
            log.debug("查询所有课程成功：结果数={}", courses.size());
            return Result.success(courses);
        } catch (Exception e) {
            log.error("查询所有课程异常：错误：{}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询课程详情
     */
    @GetMapping("/detail")
    public Result<Course> detail(@RequestParam("courseId") Long courseId) {
        log.debug("收到查询课程详情请求：URI=/api/course/detail, 参数：courseId={}", courseId);
        try {
            log.info("执行查询课程详情业务：courseId={}", courseId);
            Course course = courseService.selectById(courseId);
            log.debug("查询课程详情成功：courseId={}, courseName={}", courseId, course.getCourseName());
            return Result.success(course);
        } catch (Exception e) {
            log.error("查询课程详情异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
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
        log.debug("收到搜索课程请求：URI=/api/course/search, 参数：name={}, description={}", name, description);
        try {
            log.info("执行搜索课程业务：name={}, description={}", name, description);
            List<Course> courses = courseService.search(name, description);
            log.debug("搜索课程成功：结果数={}", courses.size());
            return Result.success(courses);
        } catch (Exception e) {
            log.error("搜索课程异常：name={}, description={}, 错误：{}", name, description, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取指定教师的所有已发布课程的统计数据（包含完成率）
     */
    @GetMapping("/stats/all")
    public Result<List<CourseStatistics>> getAllCourseStatistics(@RequestParam("teacherId") Long teacherId) {
        log.debug("收到获取课程统计数据请求：URI=/api/course/stats/all, 参数：teacherId={}", teacherId);
        try {
            log.info("执行获取课程统计数据业务：teacherId={}", teacherId);
            List<CourseStatistics> statistics = progressService.getAllCourseStatistics(teacherId);
            log.debug("获取课程统计数据成功：teacherId={}, 课程数={}", teacherId, statistics.size());
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取课程统计数据失败：teacherId={}, 错误：{}", teacherId, e.getMessage(), e);
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
        log.debug("收到发布课程请求：URI=/api/course/{courseId}/publish, 参数：courseId={}, teacherId={}", courseId, teacherId);
        try {
            log.info("执行发布课程业务：courseId={}, teacherId={}", courseId, teacherId);
            boolean success = courseService.publishCourse(courseId, teacherId);
            if (success) {
                log.debug("发布课程成功：courseId={}", courseId);
                return Result.success("课程发布成功");
            }
            log.warn("发布课程失败：courseId={}, teacherId={}", courseId, teacherId);
            return Result.error(500, "课程发布失败");
        } catch (IllegalArgumentException e) {
            log.warn("发布课程参数错误：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("发布课程业务异常：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("课程发布失败：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage(), e);
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
        log.debug("收到取消发布课程请求：URI=/api/course/{courseId}/unpublish, 参数：courseId={}, teacherId={}", courseId, teacherId);
        try {
            log.info("执行取消发布课程业务：courseId={}, teacherId={}", courseId, teacherId);
            boolean success = courseService.unpublishCourse(courseId, teacherId);
            if (success) {
                log.debug("取消发布课程成功：courseId={}", courseId);
                return Result.success("课程已取消发布");
            }
            log.warn("取消发布课程失败：courseId={}, teacherId={}", courseId, teacherId);
            return Result.error(500, "取消发布失败");
        } catch (IllegalArgumentException e) {
            log.warn("取消发布课程参数错误：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("取消发布课程业务异常：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("取消发布课程失败：courseId={}, teacherId={}, 错误：{}", courseId, teacherId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询教师的所有课程（包含草稿和已发布）
     */
    @GetMapping("/teacher/{teacherId}/all")
    public Result<List<Course>> getCoursesByTeacherId(@PathVariable("teacherId") Long teacherId) {
        log.debug("收到查询教师课程请求：URI=/api/course/teacher/{teacherId}/all, 参数：teacherId={}", teacherId);
        try {
            log.info("执行查询教师课程业务：teacherId={}", teacherId);
            List<Course> courses = courseService.getCoursesByTeacherId(teacherId);
            log.debug("查询教师课程成功：teacherId={}, 课程数={}", teacherId, courses.size());
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取教师课程列表失败：teacherId={}, 错误：{}", teacherId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 查询所有已发布的课程列表（学生端使用）
     */
    @GetMapping("/published")
    public Result<List<Course>> getPublishedCourses() {
        log.debug("收到查询已发布课程请求：URI=/api/course/published");
        try {
            log.info("执行查询已发布课程业务");
            List<Course> courses = courseService.getPublishedCourses();
            log.debug("查询已发布课程成功：结果数={}", courses.size());
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取已发布课程列表失败：错误：{}", e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 解析课程请求参数（Controller 层的参数处理职责）
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
