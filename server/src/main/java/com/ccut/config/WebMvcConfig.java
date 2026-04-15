package com.ccut.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 日志拦截器 - 拦截所有请求
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**");

        // JWT 认证拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/user/init");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/");
        log.info("Upload dir: {}", location);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        log.info("CORS允许的源: {}", allowedOrigins);

        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/uploads/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
