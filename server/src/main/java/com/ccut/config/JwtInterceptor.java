package com.ccut.config;

import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * JWT 认证拦截器
 * 负责验证请求中的 JWT 令牌，确保用户身份合法
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Value("${jwt.enabled:true}")
    private boolean jwtEnabled;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();

        // 开发环境下禁用 JWT 认证
        if (!jwtEnabled) {
            log.debug("[JWT 禁用模式] 放行请求：{} {} from {}", method, uri, remoteAddr);
            return true;
        }

        // 放行预检请求，避免浏览器 CORS 预检导致的网络错误
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.trace("放行 CORS 预检请求：{} {}", method, uri);
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // 放行登录和静态资源
        if (uri.equals("/api/auth/login") || uri.startsWith("/uploads/")) {
            log.trace("放行公开请求：{} {}", method, uri);
            return true;
        }

        // 检查 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT 认证失败] 缺少或无效的 Authorization 头：{} {} from {}", method, uri, remoteAddr);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);

        // 验证 Token 格式
        if (!JWTUtils.validateToken(token)) {
            log.warn("[JWT 认证失败] Token 无效：{} {} from {}, 错误：Token 格式错误或已过期", 
                method, uri, remoteAddr);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return false;
        }

        // 验证 token 是否与数据库一致（防止退出登录后 token 仍可使用）
        try {
            String username = JWTUtils.getUsernameFromToken(token);
            User user = userMapper.getUserByUsername(username);
            
            if (user == null) {
                log.warn("[JWT 认证失败] 用户不存在：{} {} from {}, 用户名：{}", 
                    method, uri, remoteAddr, username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked");
                return false;
            }
            
            if (!token.equals(user.getToken())) {
                log.warn("[JWT 认证失败] Token 已失效（用户可能已重新登录）：{} {} from {}, 用户名：{}", 
                    method, uri, remoteAddr, username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked");
                return false;
            }
            
            log.debug("[JWT 认证成功] 用户{}访问{} {} from {}", username, method, uri, remoteAddr);
            
        } catch (Exception e) {
            log.error("[JWT 认证异常] Token 验证过程出错：{} {} from {}, 错误：{}", 
                method, uri, remoteAddr, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token validation failed");
            return false;
        }

        // 验证通过
        return true;
    }
}
