package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.User;
import com.ccut.service.Impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/insert")
    public Result<User> insert(@RequestBody User user){
        int result = userService.insert(user);
        System.out.println(result);
        if (result > 0) {
            return Result.success(user);
        }
        return Result.error(500, "添加失败");
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody User user){
        return Result.success("登录成功");
    }
}
