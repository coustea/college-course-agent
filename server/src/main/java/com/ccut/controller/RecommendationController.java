package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Recommendation;
import com.ccut.service.RecommendationService;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程推荐控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取学生的课程推荐
     */
    @GetMapping("/list")
    public Result<List<Recommendation>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        log.debug("收到获取课程推荐请求：URI=/api/recommendation/list, 参数：limit={}", limit);
        try {
            // 从 JWT token 中获取学生 ID
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("获取课程推荐未授权访问");
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            // 这里假设 username 就是 studentId，实际需要根据实际情况调整
            Long studentId = Long.parseLong(username);

            log.info("执行获取课程推荐业务：studentId={}, limit={}", studentId, limit);
            List<Recommendation> recommendations = recommendationService.getStudentRecommendations(studentId, limit);
            log.debug("获取课程推荐成功：studentId={}, limit={}, 结果数={}", studentId, limit, recommendations.size());

            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("获取推荐失败：错误：{}", e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "获取推荐失败：" + e.getMessage());
        }
    }

    /**
     * 刷新推荐
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshRecommendations(HttpServletRequest request) {
        log.debug("收到刷新推荐请求：URI=/api/recommendation/refresh");
        try {
            // 从 JWT token 中获取学生 ID
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("刷新推荐未授权访问");
                return Result.error(401, "未授权访问");
            }
            String token = authHeader.substring(7);
            String username = JWTUtils.getUsernameFromToken(token);

            Long studentId = Long.parseLong(username);

            log.info("执行刷新推荐业务：studentId={}", studentId);
            // 刷新推荐
            recommendationService.refreshRecommendations(studentId);

            Map<String, Object> data = new HashMap<>();
            data.put("message", "推荐已刷新");
            data.put("refreshTime", System.currentTimeMillis());
            data.put("studentId", studentId);

            log.debug("刷新推荐成功：studentId={}", studentId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("刷新推荐失败：错误：{}", e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "刷新推荐失败：" + e.getMessage());
        }
    }

    /**
     * 标记推荐为已点击
     */
    @PostMapping("/{id}/click")
    public Result<String> markAsClicked(@PathVariable Long id) {
        log.debug("收到标记推荐为已点击请求：URI=/api/recommendation/{}/click, 参数：id={}", id, id);
        try {
            log.info("执行标记推荐为已点击业务：id={}", id);
            recommendationService.markRecommendationAsClicked(id);
            log.debug("标记推荐为已点击成功：id={}", id);
            return Result.success("标记成功");
        } catch (Exception e) {
            log.error("标记推荐失败：id={}, 错误：{}", id, e.getMessage(), e);
            e.printStackTrace();
            return Result.error(500, "标记失败：" + e.getMessage());
        }
    }
}
