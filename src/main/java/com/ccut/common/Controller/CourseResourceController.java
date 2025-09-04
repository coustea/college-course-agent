package com.ccut.common.Controller;


import com.ccut.common.entity.ApiResult;
import com.ccut.common.entity.CourseResource;
import com.ccut.common.service.CourseResourceService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courseResource")
public class CourseResourceController {


    @Autowired
    private CourseResourceService courseResourceService;

    @GetMapping("/{id}")
    public ApiResult<List<CourseResource>> getById(@PathVariable int id) {
        try {
            List<CourseResource> courseResources = courseResourceService.selectById(id);
            if (courseResources != null && !courseResources.isEmpty()) {
                return ApiResultHandler.success("查找成功", courseResources);
            } else {
                return ApiResultHandler.buildApiResult(404, "未找到资源", null);
            }
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,"查找失败",null);
        }
    }
    @PostMapping
    public ApiResult add(@RequestBody CourseResource courseResource) {
        try {
            int res = courseResourceService.addCourseResource(courseResource);
            if (res == 1){
                return ApiResultHandler.success("添加成功",courseResource);
            }
            else{
                return ApiResultHandler.buildApiResult(500,"添加失败",null);
            }
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,e.getMessage(),null);
        }
    }
    @PutMapping("/{id}")
    public ApiResult update(@RequestBody CourseResource courseResource, @PathVariable int id) {
        int res = courseResourceService.updateCourseResource(courseResource);
        if (res == 1){
            return ApiResultHandler.success("更新成功",courseResource);
        }
        else {
            return ApiResultHandler.buildApiResult(500,"更新失败",null);
        }
    }


    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Integer id) {
        int res = courseResourceService.deleteCourseResource(id);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200, "删除成功", null);
        }else {
            return ApiResultHandler.buildApiResult(500,"删除失败",null);
        }

    }

}
