package com.ccut.common.Controller;

import com.ccut.common.entity.*;
import com.ccut.common.service.AdminService;
import com.ccut.common.service.StudentService;
import com.ccut.common.service.TeacherService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AdminService adminService;


    @PostMapping
    public ApiResult login(@RequestBody User user) {

        if("admin".equals(user.getRole())){
            Admin admin = adminService.login(user);
            return ApiResultHandler.success("登录成功",admin);
        }else if("teacher".equals(user.getRole())){
            Teacher teacher = teacherService.login(user);
            return ApiResultHandler.success("登录成功",teacher);
        }else if("student".equals(user.getRole())){
            Student student = studentService.login(user);
            return ApiResultHandler.success("登录成功",student);
        }else{
            return ApiResultHandler.buildApiResult(500,"登录失败",null);
        }
    }

}

