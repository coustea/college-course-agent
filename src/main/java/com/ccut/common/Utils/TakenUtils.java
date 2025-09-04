package com.ccut.common.Utils;


import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ccut.common.service.StudentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TakenUtils {

    @Resource
    StudentService studentService;

    static StudentService staticStudentService;;
    @PostConstruct
    public void init() {
        staticStudentService = studentService;
    }

    /**
     *生成token
     */
    public static String createToken(String data, String sign) {
        return JWT.create().withAudience(data) // 将 userId-role 保存到 token 作为载荷
                .withExpiresAt(DateUtil.offsetHour(new Date(),2)) // 过期时间
                .sign(Algorithm.HMAC256(sign)); // 使用 HMAC256 算法签名 password作为token密钥
    }


}
