package com.ccut.plugin;

import com.ccut.context.UserContext;
import com.ccut.entity.Course;
import com.ccut.service.CourseService;
import com.ccut.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程推荐插件 — 插件化示例
 */
@Component
public class CourseRecommendationPlugin implements ToolPlugin {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private CourseService courseService;

    @Override
    public String getName() {
        return "recommend_courses";
    }

    @Override
    public String getDescription() {
        return "根据学生的兴趣和学习历史推荐相关课程。当学生表达想学新东西、寻求推荐、或感到迷茫时使用。";
    }

    @Override
    public Class<?> getRequestType() {
        return RecommendRequest.class;
    }

    @Override
    public Object execute(Object request) {
        RecommendRequest req = (RecommendRequest) request;
        Long userId = UserContext.get().userId();

        // 模拟推荐逻辑或调用现有的 recommendationService
        List<Course> recommendations = courseService.searchByName(req.interest() != null ? req.interest() : "Java");

        if (recommendations.isEmpty()) {
            return "目前没有找到与 '" + req.interest() + "' 相关的推荐课程。";
        }

        return recommendations.stream()
                .map(c -> String.format("[entity:course|%d|%s|%s|%s|%s]",
                        c.getCourseId(),
                        c.getCourseName() != null ? c.getCourseName().replace("|", " ") : "未知课程",
                        c.getDescription() != null ? c.getDescription().replace("|", " ") : "暂无简介",
                        c.getTeacher() != null ? c.getTeacher().getName() : "未知教师",
                        c.getCredits() != null ? c.getCredits() : 0))
                .collect(Collectors.joining("\n", "为你找到以下推荐课程，点击卡片查看详情：\n", ""));
    }

    public record RecommendRequest(String interest) {}
}
