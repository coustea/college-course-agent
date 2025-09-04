package com.ccut.common.Controller;

import com.ccut.common.entity.ApiResult;
import com.ccut.common.entity.Course;
import com.ccut.common.entity.Teacher;
import com.ccut.common.service.CourseService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private CourseService courseService;


    @GetMapping
    public ApiResult<List<Course>> getAllCourse(@ModelAttribute Course course) {
        List<Course> courseList = courseService.selectAll(course);
        if (courseList != null && !courseList.isEmpty()) {
            return ApiResultHandler.success("查询成功", courseList);
        } else {
            return ApiResultHandler.buildApiResult(404, "未找到任何课程", null);
        }
    }

    @PostMapping
    public ApiResult add(@RequestBody Course course) {
        try {
            List<Integer> teacherIds = course.getTeachers().stream()
                    .map(Teacher::getId)
                    .collect(Collectors.toList());

            int res = courseService.addCourse(course, teacherIds);

            if (res == 1){
                return ApiResultHandler.success("添加成功",course);
            }
            else{
                return ApiResultHandler.buildApiResult(500,"添加失败",null);
            }
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,e.getMessage(),null);
        }
    }
    @PutMapping("/{id}")
    public ApiResult update(@RequestBody Course course, @PathVariable int id) {
        if (course.getId() != id) {
            return ApiResultHandler.buildApiResult(400, "路径ID与课程ID不一致", null);
        }
        int res = courseService.updateCourse(course);
        if (res == 1) {
            return ApiResultHandler.success("更新成功", course);
        } else {
            return ApiResultHandler.buildApiResult(500, "更新失败", null);
        }
    }


    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Integer id) {
        int res = courseService.deleteCourse(id);
        if (res == 1) {
            return ApiResultHandler.success("删除成功，ID：" + id, null);
        } else {
            return ApiResultHandler.buildApiResult(404, "未找到该课程，删除失败", null);
        }
    }
}
