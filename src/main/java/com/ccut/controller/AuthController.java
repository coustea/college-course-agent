package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.utils.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody User user) {
        try {
            // 基础参数验证
            if (user.getUsername() == null || user.getUsername().isEmpty()
                    || user.getPassword() == null || user.getPassword().isEmpty()) {
                log.warn("用户名或密码为空");
                return Result.error(400, "用户名和密码不能为空");
            }

            // 查询数据库用户
            User dbUser = userMapper.getUserByUsername(user.getUsername());
            if (dbUser == null) {
                log.warn("账号不存在: {}", user.getUsername());
                return Result.error(401, "账号不存在");
            }

            //  密码校验
            if (!dbUser.getPassword().equals(user.getPassword())) {
                log.warn("密码错误: {}", user.getUsername());
                return Result.error(401, "密码错误");
            }

            // 生成 Token
            String token = JWTUtils.generateToken(dbUser.getUsername());
            log.info("用户 {} 登录成功", dbUser.getUsername());

            //返回前端的数据（用户名 + token）
            Map<String, Object> data = new HashMap<>();
            data.put("userId", dbUser.getId());
            data.put("username", dbUser.getUsername());
            data.put("token", token);


            // 更新用户 token
            dbUser.setToken(token);
            userMapper.updateUser(dbUser);

            return Result.success(data);

        } catch (Exception e) {
            log.error("登录异常", e);
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
}
