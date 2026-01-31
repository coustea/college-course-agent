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

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody User user) {
        try {
            Map<String, Object> data = authService.login(user);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            log.warn("登录参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("登录业务异常: {}", e.getMessage());
            return Result.error(401, e.getMessage());
        } catch (Exception e) {
            log.error("登录异常", e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    public Result<Object> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // 从Authorization头中提取token
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return Result.error(400, "无效的Authorization头");
            }

            String token = authorizationHeader.substring(7);
            authService.logout(token);

            return Result.success("退出登录成功");
        } catch (IllegalArgumentException e) {
            log.warn("退出登录参数错误: {}", e.getMessage());
            return Result.error(401, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("退出登录业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("退出登录异常", e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

}
