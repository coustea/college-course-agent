package com.ccut.controller;

import com.ccut.entity.Course;
import com.ccut.entity.Result;
import com.ccut.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/course")
@Slf4j
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Course> insert(@RequestParam(value = "course", required = false) String courseJson,
                                 @RequestParam(value = "courseCode", required = false) String courseCode,
                                 @RequestParam(value = "courseName", required = false) String courseName,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "teacherId", required = false) Long teacherId,
                                 @RequestPart(value = "image", required = false) MultipartFile image){
        try {
            Course course;
            if (courseJson != null && !courseJson.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                course = mapper.readValue(courseJson, Course.class);
            } else {
                course = new Course();
                course.setCourseCode(courseCode);
                course.setCourseName(courseName);
                course.setDescription(description);
                course.setTeacherId(teacherId);
            }
            if (image != null && !image.isEmpty()) {
                java.time.LocalDate date = java.time.LocalDate.now();
                java.nio.file.Path baseDir = java.nio.file.Paths.get("uploads").toAbsolutePath();
                java.nio.file.Files.createDirectories(baseDir);
                java.nio.file.Path uploadDir = baseDir.resolve(date.toString());
                java.nio.file.Files.createDirectories(uploadDir);
                String original = image.getOriginalFilename();
                String ext = null;
                if (original != null && original.contains(".")) {
                    ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                }
                String filename = java.util.UUID.randomUUID().toString().replace("-", "");
                if (ext != null && !ext.isEmpty()) filename = filename + "." + ext;
                java.nio.file.Path target = uploadDir.resolve(filename);
                java.nio.file.Files.createDirectories(target.getParent());
                image.transferTo(target.toFile());
                String url = "/uploads/" + date + "/" + filename;
                course.setResourceUrl(url);
            }
            int n = courseService.insert(course);
            if (n > 0) return Result.success(course);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(@RequestParam(value = "course", required = false) String courseJson,
                                 @RequestParam(value = "courseId", required = false) Long courseId,
                                 @RequestParam(value = "courseCode", required = false) String courseCode,
                                 @RequestParam(value = "courseName", required = false) String courseName,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "teacherId", required = false) Long teacherId,
                                 @RequestPart(value = "image", required = false) MultipartFile image){
        try {
            Course course;
            if (courseJson != null && !courseJson.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                course = mapper.readValue(courseJson, Course.class);
            } else {
                course = new Course();
                course.setCourseId(courseId);
                course.setCourseCode(courseCode);
                course.setCourseName(courseName);
                course.setDescription(description);
                course.setTeacherId(teacherId);
            }
            if (course.getCourseId() == null) return Result.error(400, "courseId 不能为空");
            if (image != null && !image.isEmpty()) {
                java.time.LocalDate date = java.time.LocalDate.now();
                java.nio.file.Path baseDir = java.nio.file.Paths.get("uploads").toAbsolutePath();
                java.nio.file.Files.createDirectories(baseDir);
                java.nio.file.Path uploadDir = baseDir.resolve(date.toString());
                java.nio.file.Files.createDirectories(uploadDir);
                String original = image.getOriginalFilename();
                String ext = null;
                if (original != null && original.contains(".")) {
                    ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                }
                String filename = java.util.UUID.randomUUID().toString().replace("-", "");
                if (ext != null && !ext.isEmpty()) filename = filename + "." + ext;
                java.nio.file.Path target = uploadDir.resolve(filename);
                java.nio.file.Files.createDirectories(target.getParent());
                image.transferTo(target.toFile());
                String url = "/uploads/" + date + "/" + filename;
                course.setResourceUrl(url);
            }
            int n = courseService.updateById(course);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 兼容：纯 JSON 插入（不上传图片）
    @PostMapping(value = "/insert", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Course> insertJson(@RequestBody Course course){
        try {
            int n = courseService.insert(course);
            if (n > 0) return Result.success(course);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 兼容：纯 JSON 更新（不上传图片）
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> updateJson(@RequestBody Course course){
        try {
            if (course.getCourseId() == null) return Result.error(400, "courseId 不能为空");
            int n = courseService.updateById(course);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("courseId") Long courseId){
        try {
            int n = courseService.deleteById(courseId);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<java.util.List<Course>> list(){
        try {
            return Result.success(courseService.selectAll());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/detail")
    public Result<Course> detail(@RequestParam("courseId") Long courseId){
        try {
            return Result.success(courseService.selectById(courseId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/search")
    public Result<java.util.List<Course>> search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description){
        try {
            return Result.success(courseService.search(name, description));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
