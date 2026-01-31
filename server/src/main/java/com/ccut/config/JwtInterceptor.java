package com.ccut.config;

import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;
    
    @Value("${jwt.enabled:true}")
    private boolean jwtEnabled;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 开发环境下禁用JWT认证
        if (!jwtEnabled) {
            return true;
        }
        
        // 放行预检请求，避免浏览器 CORS 预检导致的网络错误
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        String uri = request.getRequestURI();

        // 放行登录和静态资源
        if (uri.equals("/api/auth/login") || uri.startsWith("/uploads/")) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return false;
        }
        String token = authHeader.substring(7);
        if (!JWTUtils.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return false;
        }
        
        // 验证token是否与数据库一致（防止退出登录后token仍可使用）
        try {
            String username = JWTUtils.getUsernameFromToken(token);
            User user = userMapper.getUserByUsername(username);
            if (user == null || !token.equals(user.getToken())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked");
                return false;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token validation failed");
            return false;
        }
        
        // 验证通过
        return true;
    }
}
