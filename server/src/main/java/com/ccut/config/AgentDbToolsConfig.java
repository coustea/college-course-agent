package com.ccut.config;

import com.ccut.context.UserContext;
import com.ccut.entity.Course;
import com.ccut.entity.Enrollment;
import com.ccut.entity.Student;
import com.ccut.entity.User;
import com.ccut.service.*;
import com.ccut.dto.StudentStatistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent 数据库查询工具注册配置类
 *
 * <p>将 6 个数据库查询能力注册为 Spring AI 标准的 Function Tool，
 * 供 ChatClient 通过 .defaultTools(...) 挂载给大模型自动调用。</p>
 *
 * <p>所有工具均为只读查询，通过 {@link UserContext} 获取当前用户身份。
 * 出错时返回 JSON 错误信息而不抛异常，防止中断 ChatClient 的 tool call 循环。</p>
 */
@Configuration
public class AgentDbToolsConfig {

    private static final Logger logger = LoggerFactory.getLogger(AgentDbToolsConfig.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ======================== Tool 1: 查询学生个人信息 ========================

    @Bean
    @Description("查询当前登录学生的个人信息（姓名、学号、班级、专业、年级等）。" +
            "当学生询问个人信息时自动使用。仅对教师角色返回提示不可用。")
    public Function<EmptyRequest, String> queryStudentProfile(StudentService studentService) {
        return request -> {
            try {
                UserContext.Context ctx = UserContext.get();
                if (ctx == null) return error("无法获取用户上下文");

                if (ctx.role() != User.Role.student) {
                    return error("当前用户是教师，无法查询学生个人信息");
                }

                Student student = studentService.selectById(ctx.userId());
                if (student == null) return error("未找到学生信息");

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("姓名", student.getName());
                data.put("学号", student.getStudentNumber());
                data.put("班级", student.getClassName());
                data.put("专业", student.getMajor());
                data.put("年级", student.getGrade());
                data.put("邮箱", student.getEmail());
                data.put("手机", student.getPhone());
                return success(data);
            } catch (Exception e) {
                logger.error("queryStudentProfile 错误: {}", e.getMessage(), e);
                return error("查询学生信息失败: " + e.getMessage());
            }
        };
    }

    // ======================== Tool 2: 查询已选课程及进度概览 ========================

    @Bean
    @Description("查询当前登录学生已选的所有课程及各课程的学习进度概览。" +
            "当学生询问\"我选了哪些课\"、\"课程进度怎么样\"、\"学习概况\"等问题时使用。")
    public Function<EmptyRequest, String> queryMyCourses(
            EnrollmentService enrollmentService,
            ProgressService progressService,
            CourseService courseService) {
        return request -> {
            try {
                UserContext.Context ctx = UserContext.get();
                if (ctx == null) return error("无法获取用户上下文");

                if (ctx.role() != User.Role.student) {
                    return error("当前用户是教师，无法查询选课信息");
                }

                List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(ctx.userId());
                if (enrollments == null || enrollments.isEmpty()) {
                    return success(Map.of("提示", "当前没有已选课程"));
                }

                List<Map<String, Object>> courses = new java.util.ArrayList<>();
                for (Enrollment enrollment : enrollments) {
                    Map<String, Object> courseInfo = new LinkedHashMap<>();
                    if (enrollment.getCourse() != null) {
                        Course c = enrollment.getCourse();
                        courseInfo.put("课程名称", c.getCourseName());
                        courseInfo.put("学分", c.getCredits());
                        courseInfo.put("授课教师", c.getTeacher() != null ? c.getTeacher().getName() : "未知");
                        courseInfo.put("学期", c.getSemester());
                        courseInfo.put("选课状态", enrollment.getStatus().name());

                        // 获取该课程的学习进度
                        try {
                            Map<String, Object> progress = progressService.getAllProgress(ctx.userId(), c.getCourseId());
                            if (progress != null) {
                                courseInfo.put("学习进度", progress);
                            }
                        } catch (Exception e) {
                            logger.debug("获取课程 {} 进度失败: {}", c.getCourseName(), e.getMessage());
                        }
                    }
                    courses.add(courseInfo);
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("已选课程数", courses.size());
                result.put("课程列表", courses);
                return success(result);
            } catch (Exception e) {
                logger.error("queryMyCourses 错误: {}", e.getMessage(), e);
                return error("查询选课信息失败: " + e.getMessage());
            }
        };
    }

    // ======================== Tool 3: 查询某课程的详细学习进度 ========================

    @Bean
    @Description("查询某门课程的详细学习进度，包括视频观看进度、文档阅读进度等。" +
            "当学生询问某门具体课程的学习进度、视频看了多少、文档读到哪里时使用。" +
            "参数 courseNameOrId 可以为课程名称或课程ID。")
    public Function<CourseNameRequest, String> queryLearningProgress(
            ProgressService progressService,
            CourseService courseService) {
        return request -> {
            try {
                UserContext.Context ctx = UserContext.get();
                if (ctx == null) return error("无法获取用户上下文");

                if (ctx.role() != User.Role.student) {
                    return error("当前用户是教师，无法查询学习进度");
                }

                Long courseId = resolveCourseId(request.courseNameOrId(), courseService);
                if (courseId == null) {
                    return error("未找到课程: " + request.courseNameOrId());
                }

                Map<String, Object> progress = progressService.getAllProgress(ctx.userId(), courseId);
                if (progress == null || progress.isEmpty()) {
                    return success(Map.of("提示", "该课程暂无学习记录"));
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("课程ID", courseId);
                result.put("课程名称", request.courseNameOrId());
                result.put("学习进度详情", progress);
                return success(result);
            } catch (Exception e) {
                logger.error("queryLearningProgress 错误: {}", e.getMessage(), e);
                return error("查询学习进度失败: " + e.getMessage());
            }
        };
    }

    // ======================== Tool 4: 查询错题本及统计 ========================

    @Bean
    @Description("查询当前学生的错题本及错题统计数据。" +
            "当学生询问\"我的错题\"、\"哪些题做错了\"、\"错题统计\"等问题时使用。" +
            "参数 courseNameOrId 可选，不传则查询所有课程的错题。")
    public Function<CourseNameOptionalRequest, String> queryWrongQuestions(
            WrongQuestionService wrongQuestionService,
            CourseService courseService) {
        return request -> {
            try {
                UserContext.Context ctx = UserContext.get();
                if (ctx == null) return error("无法获取用户上下文");

                if (ctx.role() != User.Role.student) {
                    return error("当前用户是教师，无法查询错题本");
                }

                Long courseId = null;
                String courseName = null;
                if (request.courseNameOrId() != null && !request.courseNameOrId().isBlank()) {
                    courseId = resolveCourseId(request.courseNameOrId(), courseService);
                    courseName = request.courseNameOrId();
                    if (courseId == null) {
                        return error("未找到课程: " + request.courseNameOrId());
                    }
                }

                Map<String, Object> result = new LinkedHashMap<>();

                // 查询错题统计
                Map<String, Object> stats = wrongQuestionService.getStatistics(ctx.userId(), courseId);
                if (stats != null) {
                    result.put("错题统计", stats);
                }

                // 查询错题列表（限制数量避免过大）
                List<Map<String, Object>> wrongQuestions = wrongQuestionService.getWrongQuestions(ctx.userId(), courseId);
                if (wrongQuestions != null && !wrongQuestions.isEmpty()) {
                    // 限制返回数量，只返回前 20 条
                    List<Map<String, Object>> limitedList = wrongQuestions.stream()
                            .limit(20)
                            .collect(Collectors.toList());
                    result.put("错题列表（前20条）", limitedList);
                    if (wrongQuestions.size() > 20) {
                        result.put("总计错题数", wrongQuestions.size());
                    }
                } else {
                    result.put("提示", "暂无错题记录，继续加油！");
                }

                if (courseName != null) {
                    result.put("筛选课程", courseName);
                }

                return success(result);
            } catch (Exception e) {
                logger.error("queryWrongQuestions 错误: {}", e.getMessage(), e);
                return error("查询错题本失败: " + e.getMessage());
            }
        };
    }

    // ======================== Tool 5: 查询学习统计 ========================

    @Bean
    @Description("查询当前学生的整体学习统计数据，包括在修课程数、本周学习时长、连续打卡天数、" +
            "最近几周每周学习时长等。当学生询问\"我的学习情况\"、\"学了多久\"、\"学习统计\"等问题时使用。")
    public Function<EmptyRequest, String> queryStudyStatistics(ProgressService progressService) {
        return request -> {
            try {
                UserContext.Context ctx = UserContext.get();
                if (ctx == null) return error("无法获取用户上下文");

                if (ctx.role() != User.Role.student) {
                    return error("当前用户是教师，无法查询学习统计");
                }

                Map<String, Object> result = new LinkedHashMap<>();

                // 查询整体统计
                StudentStatistics stats = progressService.getStudentStatistics(ctx.userId());
                if (stats != null) {
                    result.put("在修课程数", stats.getCourseCount());
                    result.put("本周学习时长（小时）", stats.getWeeklyStudyHours());
                    result.put("连续打卡天数", stats.getConsecutiveDays());
                }

                // 查询最近 4 周每周学习时长
                List<Map<String, Object>> weeklyTime = progressService.getStudentRecentWeeksTime(ctx.userId(), 4);
                if (weeklyTime != null && !weeklyTime.isEmpty()) {
                    result.put("近4周每周学习时长", weeklyTime);
                }

                return success(result);
            } catch (Exception e) {
                logger.error("queryStudyStatistics 错误: {}", e.getMessage(), e);
                return error("查询学习统计失败: " + e.getMessage());
            }
        };
    }

    // ======================== Tool 6: 查询 AI 考试记录及成绩 ========================

    @Bean
    @Description("查询当前学生的 AI 考试记录、正确率和平均成绩。" +
            "当学生询问\"我的考试成绩\"、\"考试记录\"、\"正确率\"等问题时使用。" +
            "参数 courseNameOrId 可选，不传则查询所有课程的考试记录。")
    public Function<CourseNameOptionalRequest, String> queryExamHistory(
            AiExamService aiExamService,
            CourseService courseService) {
        return request -> {
            try {
                UserContext.Context ctx = UserContext.get();
                if (ctx == null) return error("无法获取用户上下文");

                if (ctx.role() != User.Role.student) {
                    return error("当前用户是教师，无法查询考试记录");
                }

                Long courseId = null;
                String courseName = null;
                if (request.courseNameOrId() != null && !request.courseNameOrId().isBlank()) {
                    courseId = resolveCourseId(request.courseNameOrId(), courseService);
                    courseName = request.courseNameOrId();
                    if (courseId == null) {
                        return error("未找到课程: " + request.courseNameOrId());
                    }
                }

                Map<String, Object> result = new LinkedHashMap<>();

                // 查询考试记录
                List<?> exams = aiExamService.listExams(ctx.userId(), courseId);
                if (exams != null && !exams.isEmpty()) {
                    result.put("考试记录", exams);
                    result.put("考试次数", exams.size());
                } else {
                    result.put("提示", "暂无 AI 考试记录");
                }

                // 查询正确率和平均成绩
                Map<String, Object> accuracy = aiExamService.getAccuracy(ctx.userId(), courseId);
                if (accuracy != null) {
                    result.put("正确率统计", accuracy);
                }

                Map<String, Object> avgScore = aiExamService.getAverageScore(ctx.userId(), courseId);
                if (avgScore != null) {
                    result.put("平均成绩", avgScore);
                }

                if (courseName != null) {
                    result.put("筛选课程", courseName);
                }

                return success(result);
            } catch (Exception e) {
                logger.error("queryExamHistory 错误: {}", e.getMessage(), e);
                return error("查询考试记录失败: " + e.getMessage());
            }
        };
    }

    // ======================== 请求类型定义 ========================

    /**
     * 无参数请求（用于不需要参数的工具）
     */
    public record EmptyRequest() {}

    /**
     * 课程名称或 ID 请求
     */
    public record CourseNameRequest(
            @ToolParam(description = "课程名称或课程ID，用于定位具体课程")
            String courseNameOrId
    ) {}

    /**
     * 可选课程名称或 ID 请求（参数可选）
     */
    public record CourseNameOptionalRequest(
            @ToolParam(description = "课程名称或课程ID（可选），不传则查询所有课程")
            String courseNameOrId
    ) {}

    // ======================== 工具方法 ========================

    /**
     * 根据课程名称或 ID 解析为课程 ID。
     * 如果输入是纯数字则视为课程 ID，否则按课程名称搜索。
     */
    private Long resolveCourseId(String courseNameOrId, CourseService courseService) {
        if (courseNameOrId == null || courseNameOrId.isBlank()) return null;

        // 尝试按 ID 查询
        try {
            long id = Long.parseLong(courseNameOrId.trim());
            Course course = courseService.selectById(id);
            if (course != null) return id;
        } catch (NumberFormatException ignored) {
            // 不是纯数字，按名称搜索
        }

        // 按名称搜索
        List<Course> courses = courseService.searchByName(courseNameOrId.trim());
        if (courses != null && !courses.isEmpty()) {
            return courses.get(0).getCourseId();
        }

        return null;
    }

    /**
     * 构建成功响应 JSON
     */
    private String success(Object data) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("data", data);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":true,\"data\":\"" + data.toString().replace("\"", "\\\"") + "\"}";
        }
    }

    /**
     * 构建错误响应 JSON
     */
    private String error(String message) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", message);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
        }
    }
}
