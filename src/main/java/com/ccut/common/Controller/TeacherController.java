package com.ccut.common.Controller;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.ccut.common.entity.ApiResult;
import com.ccut.common.entity.Teacher;
import com.ccut.common.service.TeacherService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @GetMapping
    public ApiResult<List<Teacher>> getAllStudent(@ModelAttribute Teacher teacher) {
        try {
            if (teacher == null) {
                return ApiResultHandler.success("查询成功", teacherService.selectAll(null));
            }
            return ApiResultHandler.success("查询成功",teacherService.selectAll(teacher));
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,"查询失败",null);
        }
    }

    @PutMapping("/{id}")
    public ApiResult update(@RequestBody Teacher teacher, @PathVariable Integer id) {
        if (teacher.getId() == null) {
            teacher.setId(id);
        } else if (!teacher.getId().equals(id)) {
            return ApiResultHandler.buildApiResult(400, "路径ID与请求体ID不一致", null);
        }
        int res = teacherService.updateTeacher(teacher);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200,"更新成功",teacher);
        }
        else {
            return ApiResultHandler.buildApiResult(400,"更新失败",null);
        }
    }


    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Integer id) {
        int res = teacherService.deleteTeacher(id);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200, "删除成功", null);
        }else {
            return ApiResultHandler.buildApiResult(400,"删除失败",null);
        }

    }


    @PostMapping
    public ApiResult add(@RequestBody Teacher teacher){
        int res = teacherService.addTeacher(teacher);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200,"添加成功",teacher);
        }
        else {
            return ApiResultHandler.buildApiResult(400,"添加失败",null);
        }
    }

    @PostMapping("/import")
    public ApiResult<String> importData(MultipartFile file) throws Exception{
        InputStream inputStream = file.getInputStream();
        ExcelReader reader = ExcelUtil.getReader(inputStream);
        reader.addHeaderAlias("账号", "username");
        reader.addHeaderAlias("密码", "password");
        reader.addHeaderAlias("学院", "academe");
        reader.addHeaderAlias("姓名", "name");

        List<Teacher> teachers = reader.readAll(Teacher.class);

        for (Teacher teacher : teachers) {
            teacherService.addTeacher(teacher);
        }
        return ApiResultHandler.success("添加成功",teachers);
    }
}
