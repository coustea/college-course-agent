package com.ccut.controller;

import com.ccut.entity.Course;
import com.ccut.entity.Result;
import com.ccut.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/course")
@Slf4j
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Course> insert(
            @RequestParam(value = "course", required = false) String courseJson,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            Course course;

            course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);

            if (image != null && !image.isEmpty()) {
                String url = handleFileUpload(image);
                course.setResourceUrl(url);
            }

            int n = courseService.insert(course);
            if (n > 0) {
                return Result.success(course);
            }
            return Result.error(500, "添加失败");

        } catch (Exception e) {
            log.error("课程上传失败: {}", e.getMessage(), e);
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
            Course course = parseCourseFromRequest(courseJson, courseCode, courseName, description, teacherId);
            course.setCourseId(courseId);
            
            if (course.getCourseId() == null) return Result.error(400, "courseId 不能为空");
            
            if (image != null && !image.isEmpty()) {
                String url = handleFileUpload(image);
                course.setResourceUrl(url);
            }
            
            int n = courseService.updateById(course);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            log.error("课程更新失败: {}", e.getMessage(), e);
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
    
    // 提取公共方法：解析课程请求参数
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
    
    // 提取公共方法：处理文件上传
    private String handleFileUpload(MultipartFile image) throws Exception {
        LocalDate date = LocalDate.now();
        
        Path baseDir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(baseDir);
        
        Path uploadDateDir = baseDir.resolve(date.toString());
        Files.createDirectories(uploadDateDir);
        
        String original = image.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1)
                : "";
        
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (!ext.isEmpty()) {
            filename += "." + ext;
        }
        
        Path target = uploadDateDir.resolve(filename);
        image.transferTo(target.toFile());
        
        return "/uploads/" + date + "/" + filename;
    }
}
