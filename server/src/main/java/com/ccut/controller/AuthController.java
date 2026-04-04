package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.User;
import com.ccut.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Object> login(@RequestBody User user) {
        Map<String, Object> data = authService.login(user);
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Object> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("无效的 Authorization 头");
        }
        String token = authorizationHeader.substring(7);
        authService.logout(token);
        return Result.success("退出登录成功");
    }
}
