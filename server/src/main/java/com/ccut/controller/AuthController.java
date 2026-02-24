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
        log.debug("收到登录请求：URI=/api/auth/login, 参数：username={}", user.getUsername());
        try {
            log.info("执行登录业务：username={}", user.getUsername());
            Map<String, Object> data = authService.login(user);
            log.debug("登录成功，返回响应：username={}, hasToken={}", user.getUsername(), data.get("token") != null);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            log.warn("登录参数错误：username={}, 错误：{}", user.getUsername(), e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("登录业务异常：username={}, 错误：{}", user.getUsername(), e.getMessage());
            return Result.error(401, e.getMessage());
        } catch (Exception e) {
            log.error("登录异常：username={}, 错误：{}", user.getUsername(), e.getMessage(), e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    public Result<Object> logout(@RequestHeader("Authorization") String authorizationHeader) {
        log.debug("收到退出登录请求：URI=/api/auth/logout, hasToken={}", authorizationHeader != null && authorizationHeader.startsWith("Bearer "));
        try {
            // 从 Authorization 头中提取 token
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.warn("退出登录失败：无效的 Authorization 头");
                return Result.error(400, "无效的 Authorization 头");
            }

            String token = authorizationHeader.substring(7);
            log.info("执行退出登录业务：token={}...", token.substring(0, Math.min(10, token.length())));
            authService.logout(token);
            log.debug("退出登录成功");
            return Result.success("退出登录成功");
        } catch (IllegalArgumentException e) {
            log.warn("退出登录参数错误：{}", e.getMessage());
            return Result.error(401, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("退出登录业务异常：{}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("退出登录异常：错误：{}", e.getMessage(), e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

}
