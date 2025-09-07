package com.ccut.controller;

import com.ccut.entity.Course;
import com.ccut.entity.Result;
import com.ccut.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/course")
@Slf4j
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping("/insert")
    public Result<String> insert(@RequestBody Course course){
        try {
            int n = courseService.insert(course);
            if (n > 0) return Result.success("添加成功");
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Course course){
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
}
