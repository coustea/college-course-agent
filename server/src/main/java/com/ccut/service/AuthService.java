package com.ccut.service;

import com.ccut.entity.User;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * @param user 用户登录信息
     * @return 登录结果数据（包含token、用户信息等）
     * @throws IllegalArgumentException 用户名或密码为空
     * @throws RuntimeException 账号不存在、密码错误、角色不匹配等
     */
    Map<String, Object> login(User user);

    /**
     * 用户退出登录
     * @param token JWT Token
     * @throws IllegalArgumentException Token无效
     * @throws RuntimeException 用户不存在
     */
    void logout(String token);

}
