package com.ccut.common.Controller;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.ccut.common.entity.Admin;
import com.ccut.common.entity.ApiResult;
import com.ccut.common.entity.Student;
import com.ccut.common.service.AdminService;
import com.ccut.common.service.StudentService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentService studentService;

    @GetMapping
    public ApiResult<List<Admin>> getAllStudent(@ModelAttribute Admin admin) {
        try {
            if (admin == null) {
                return ApiResultHandler.success("查询成功", adminService.selectAll(null));
            }
            return ApiResultHandler.success("查询成功",adminService.selectAll(admin));
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,"查询失败",null);
        }
    }

    @PutMapping("/{id}")
    public ApiResult update(@RequestBody Admin admin) {
        int res = adminService.updateAdmin(admin);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200,"更新成功",admin);
        }
        else {
            return ApiResultHandler.buildApiResult(400,"更新失败",null);
        }
    }


    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Integer id) {
        int res = adminService.deleteAdmin(id);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200, "删除成功", null);
        }else {
            return ApiResultHandler.buildApiResult(400,"删除失败",null);
        }

    }


    @PostMapping
    public ApiResult add(@RequestBody Admin admin){
        int res = adminService.addAdmin(admin);
        if (res == 1){
            return ApiResultHandler.buildApiResult(200,"添加成功",admin);
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
        reader.addHeaderAlias("班级", "classname");
        reader.addHeaderAlias("姓名", "name");

        List<Student> students = reader.readAll(Student.class);

        for (Student student : students) {
            studentService.addStudent(student);
        }
        return ApiResultHandler.success("添加成功",students);
    }


}