package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.Recommendation;
import com.ccut.exception.BusinessException;
import com.ccut.service.RecommendationService;
import com.ccut.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程推荐控制器
 */
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未授权访问");
        }
        String token = authHeader.substring(7);
        String username = JWTUtils.getUsernameFromToken(token);

        // 这里假设 username 就是 studentId，实际需要根据实际情况调整
        Long studentId = Long.parseLong(username);

        List<Recommendation> recommendations = recommendationService.getStudentRecommendations(studentId, limit);
        return Result.success(recommendations);
    }

    /**
     * 刷新推荐
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshRecommendations(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未授权访问");
        }
        String token = authHeader.substring(7);
        String username = JWTUtils.getUsernameFromToken(token);

        Long studentId = Long.parseLong(username);

        recommendationService.refreshRecommendations(studentId);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "推荐已刷新");
        data.put("refreshTime", System.currentTimeMillis());
        data.put("studentId", studentId);

        return Result.success(data);
    }

    /**
     * 标记推荐为已点击
     */
    @PostMapping("/{id}/click")
    public Result<String> markAsClicked(@PathVariable Long id) {
        recommendationService.markRecommendationAsClicked(id);
        return Result.success("标记成功");
    }
}
