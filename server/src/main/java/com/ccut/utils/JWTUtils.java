package com.ccut.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 简化版 JWT 工具类 —— 仅使用 username 生成和解析 Token
 * 提供 Token 生成、验证和解析功能
 */
@Slf4j
@Component
public class JWTUtils {

    // Token 过期时间（7 天）- 开发环境设置较长，避免频繁过期
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000;

    // 签名密钥 (至少 64 字节以满足 HS512 算法要求)
    private static final String SIGN_KEY = "CCUT_JWT_SECRET_KEY_2024_HS512_ALGORITHM_MUST_BE_AT_LEAST_64_BYTES_LONG_FOR_SECURITY";
    private static final byte[] SECRET_KEY = SIGN_KEY.getBytes(StandardCharsets.UTF_8);

    /**
     * 生成 Token（只使用用户名）
     * @param username 用户名
     * @return JWT Token 字符串
     */
    public static String generateToken(String username) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRE);
        
        String token = Jwts.builder()
                .setSubject(username) // 仅存储用户名
                .setIssuedAt(now) // 签发时间
                .setExpiration(expirationDate) // 过期时间
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY) // 签名算法
                .compact();
        
        log.info("生成 JWT Token: 用户名={}, 过期时间={}", username, expirationDate);
        log.debug("Token 前缀：{}", token.substring(0, Math.min(20, token.length())) + "...");
        
        return token;
    }

    /**
     * 验证 Token 是否有效
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token);
            log.trace("Token 验证成功");
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token 验证失败：Token 已过期");
            return false;
        } catch (UnsupportedJwtException e) {
            log.debug("Token 验证失败：不支持的 Token 格式");
            return false;
        } catch (MalformedJwtException e) {
            log.debug("Token 验证失败：Token 格式错误");
            return false;
        } catch (SignatureException e) {
            log.debug("Token 验证失败：签名验证失败");
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("Token 验证失败：参数非法");
            return false;
        } catch (Exception e) {
            log.debug("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中提取用户名
     * @param token JWT Token
     * @return 用户名
     * @throws RuntimeException 当 Token 无效时抛出
     */
    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            String username = claims.getSubject();
            log.trace("从 Token 中解析用户名：{}", username);
            return username;
        } catch (ExpiredJwtException e) {
            log.warn("无法从 Token 中解析用户名：Token 已过期");
            throw new RuntimeException("Token 已过期", e);
        } catch (UnsupportedJwtException e) {
            log.warn("无法从 Token 中解析用户名：不支持的 Token 格式");
            throw new RuntimeException("不支持的 Token 格式", e);
        } catch (MalformedJwtException e) {
            log.warn("无法从 Token 中解析用户名：Token 格式错误");
            throw new RuntimeException("Token 格式错误", e);
        } catch (SignatureException e) {
            log.warn("无法从 Token 中解析用户名：签名验证失败");
            throw new RuntimeException("签名验证失败", e);
        } catch (Exception e) {
            log.warn("无法从 Token 中解析用户名：{}", e.getMessage());
            throw new RuntimeException("无法从令牌中解析用户名", e);
        }
    }

    /**
     * 从 HttpServletRequest 中解析当前用户名
     * @param request HTTP 请求
     * @return 用户名
     * @throws IllegalArgumentException 当 Authorization 头缺失或格式错误时
     */
    public static String resolveUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未授权访问");
        }
        String token = authHeader.substring(7);
        String username = getUsernameFromToken(token);
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("无法获取用户信息");
        }
        return username;
    }

    /**
     * 获取 Token 的过期时间
     * @param token JWT Token
     * @return 过期时间，如果 Token 无效则返回 null
     */
    public static Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            log.debug("无法获取 Token 过期时间：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查 Token 是否即将过期
     * @param token JWT Token
     * @param thresholdMillis 阈值（毫秒）
     * @return true-即将过期，false-未即将过期或 Token 无效
     */
    public static boolean isTokenAboutToExpire(String token, long thresholdMillis) {
        Date expirationDate = getExpirationDateFromToken(token);
        if (expirationDate == null) {
            return false;
        }
        long timeRemaining = expirationDate.getTime() - System.currentTimeMillis();
        boolean aboutToExpire = timeRemaining < thresholdMillis;
        if (aboutToExpire) {
            log.debug("Token 即将过期，剩余时间：{}ms", timeRemaining);
        }
        return aboutToExpire;
    }
}
