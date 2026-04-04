package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.User;
import com.ccut.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/insert")
    public Result<User> insert(@RequestBody User user) {
        int result = userService.insert(user);
        if (result > 0) {
            return Result.success(user);
        }
        throw new RuntimeException("添加失败");
    }

    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable("id") Long id, @RequestBody User user) {
        user.setId(id);
        int res = userService.updateUser(user);
        if (res > 0) {
            return Result.success("更新成功");
        }
        throw new RuntimeException("更新用户失败");
    }

    @PostMapping("/excel")
    public Result<String> insertByExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String result = userService.importStudentsFromExcel(file);
        return Result.success(result);
    }

    @DeleteMapping
    public Result<String> deleteAll() {
        int res = userService.deleteAll();
        return Result.success("删除成功");
    }
}
