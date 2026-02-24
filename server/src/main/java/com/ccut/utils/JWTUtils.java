package com.ccut.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 简化版 JWT 工具类 —— 仅使用 username 生成和解析 Token
 */
@Component
public class JWTUtils {

    // Token 过期时间（7天）- 开发环境设置较长，避免频繁过期
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000;

    // 签名密钥
    private static final String SIGN_KEY = "HSyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9";
    private static final byte[] SECRET_KEY = SIGN_KEY.getBytes(StandardCharsets.UTF_8);

    /**
     * ✅ 生成 Token（只使用用户名）
     */
    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // 仅存储用户名
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE)) // 过期时间
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY) // 签名算法
                .compact();
    }

    /**
     * ✅ 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ✅ 从 Token 中提取用户名
     */
    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject(); // 返回用户名
        } catch (Exception e) {
            throw new RuntimeException("无法从令牌中解析用户名", e);
        }
    }
}
