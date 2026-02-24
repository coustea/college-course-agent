package com.ccut.service.Impl;

import com.ccut.entity.*;
import com.ccut.mapper.*;
import com.ccut.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现
 * 使用协同过滤和基于内容的推荐算法
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private LearningProgressMapper progressMapper;

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 为学生生成课程推荐
     */
    @Override
    @Transactional
    public List<Recommendation> generateRecommendations(Long studentId, int limit) {
        log.debug("执行方法：generateRecommendations, 参数：studentId={}, limit={}", studentId, limit);
        long startTime = System.currentTimeMillis();
        try {
            List<Recommendation> recommendations = new ArrayList<>();

            // 1. 获取学生已学习的课程 ID 列表
            List<Long> enrolledCourseIds = getEnrolledCourseIds(studentId);
            log.info("获取学生已选课程：studentId={}, count={}", studentId, enrolledCourseIds.size());

            // 2. 获取所有可用课程
            List<Course> allCourses = courseMapper.findAll();
            List<Course> candidateCourses = allCourses.stream()
                    .filter(course -> !enrolledCourseIds.contains(course.getCourseId()))
                    .collect(Collectors.toList());
            log.info("筛选候选课程：total={}, candidate={}", allCourses.size(), candidateCourses.size());

            // 3. 基于不同策略生成推荐
            recommendations.addAll(generateContentBasedRecommendations(studentId, candidateCourses, enrolledCourseIds));
            recommendations.addAll(generateCollaborativeRecommendations(studentId, candidateCourses, enrolledCourseIds));
            recommendations.addAll(generatePopularRecommendations(candidateCourses));
            log.info("生成推荐：contentBased={}, collaborative={}, popular={}, total={}", 
                    recommendations.stream().filter(r -> "CONTENT_BASED".equals(r.getRecommendationType())).count(),
                    recommendations.stream().filter(r -> "COLLABORATIVE".equals(r.getRecommendationType())).count(),
                    recommendations.stream().filter(r -> "POPULAR".equals(r.getRecommendationType())).count(),
                    recommendations.size());

            // 4. 去重并按分数排序
            Map<Long, Recommendation> uniqueRecommendations = new LinkedHashMap<>();
            for (Recommendation rec : recommendations) {
                Long courseId = rec.getCourseId();
                if (!uniqueRecommendations.containsKey(courseId)) {
                    uniqueRecommendations.put(courseId, rec);
                } else {
                    // 如果已存在，保留分数更高的
                    if (rec.getScore() > uniqueRecommendations.get(courseId).getScore()) {
                        uniqueRecommendations.put(courseId, rec);
                    }
                }
            }

            // 5. 按分数降序排序并限制数量
            List<Recommendation> sortedRecommendations = uniqueRecommendations.values().stream()
                    .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            // 6. 批量保存推荐结果
            LocalDateTime now = LocalDateTime.now();
            for (Recommendation rec : sortedRecommendations) {
                rec.setStudentId(studentId);
                rec.setCreatedAt(now);
                rec.setHasClicked(false);
                recommendationMapper.insert(rec);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("生成推荐成功：studentId={}, finalCount={}, 耗时={}ms", studentId, sortedRecommendations.size(), duration);
            log.debug("方法返回：result count={}", sortedRecommendations.size());
            return sortedRecommendations;
        } catch (Exception e) {
            log.error("生成推荐失败：studentId={}, limit={}, error={}", studentId, limit, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 基于内容的推荐（根据课程标签和类别）
     */
    private List<Recommendation> generateContentBasedRecommendations(Long studentId,
                                                                     List<Course> candidateCourses,
                                                                     List<Long> enrolledCourseIds) {
        log.debug("执行方法：generateContentBasedRecommendations, 参数：studentId={}, candidateCount={}", studentId, candidateCourses.size());
        List<Recommendation> recommendations = new ArrayList<>();

        // 获取学生已学习课程的类别和标签
        Set<String> enrolledCategories = getEnrolledCategories(enrolledCourseIds);
        log.debug("已学习课程类别：{}", enrolledCategories);

        for (Course course : candidateCourses) {
            double score = 0.0;
            String reason = "";

            // 匹配课程类别
            if (enrolledCategories.contains(course.getCourseCode())) {
                score += 0.6;
                reason = "基于您学习的" + course.getCourseCode() + "类课程推荐";
            }

            // 匹配课程描述关键词
            if (course.getDescription() != null) {
                String desc = course.getDescription().toLowerCase();
                if (desc.contains("基础") || desc.contains("入门")) {
                    score += 0.2;
                    reason = reason.isEmpty() ? "基础课程推荐" : reason + "（基础课程）";
                }
            }

            // 新课加分（使用 publishedAt 判断）
            if (course.getPublishedAt() != null) {
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                // 将 Date 转换为 LocalDateTime
                LocalDateTime publishedDateTime = convertToLocalDateTime(course.getPublishedAt());
                if (publishedDateTime.isAfter(thirtyDaysAgo)) {
                    score += 0.2;
                    if (!reason.isEmpty()) reason += "，新课推荐";
                }
            }

            if (score > 0) {
                Recommendation rec = new Recommendation();
                rec.setCourseId(course.getCourseId());
                rec.setScore(score);
                rec.setReason(reason);
                rec.setRecommendationType("CONTENT_BASED");
                recommendations.add(rec);
            }
        }

        log.info("基于内容推荐生成：studentId={}, count={}", studentId, recommendations.size());
        return recommendations;
    }

    /**
     * 协同过滤推荐（基于相似学生的学习偏好）
     */
    private List<Recommendation> generateCollaborativeRecommendations(Long studentId,
                                                                       List<Course> candidateCourses,
                                                                       List<Long> enrolledCourseIds) {
        log.debug("执行方法：generateCollaborativeRecommendations, 参数：studentId={}", studentId);
        List<Recommendation> recommendations = new ArrayList();

        // 找到学习过相似课程的其他学生
        List<Long> similarStudentIds = findSimilarStudents(studentId, enrolledCourseIds);
        log.info("找到相似学生：studentId={}, similarStudentCount={}", studentId, similarStudentIds.size());

        // 统计这些学生学习但当前学生未学习的课程
        Map<Long, Integer> coursePopularity = new HashMap<>();
        for (Long similarStudentId : similarStudentIds) {
            List<Long> similarStudentCourses = getEnrolledCourseIds(similarStudentId);
            for (Long courseId : similarStudentCourses) {
                if (!enrolledCourseIds.contains(courseId)) {
                    coursePopularity.put(courseId, coursePopularity.getOrDefault(courseId, 0) + 1);
                }
            }
        }

        // 生成推荐
        int maxPopularity = coursePopularity.values().stream().max(Integer::compare).orElse(1);

        for (Map.Entry<Long, Integer> entry : coursePopularity.entrySet()) {
            Long courseId = entry.getKey();
            int popularity = entry.getValue();

            // 计算推荐分数（归一化）
            double score = (double) popularity / maxPopularity * 0.8;

            // 确保课程在候选列表中
            boolean existsInCandidates = candidateCourses.stream()
                    .anyMatch(c -> c.getCourseId().equals(courseId));

            if (existsInCandidates && score > 0.1) {
                Recommendation rec = new Recommendation();
                rec.setCourseId(courseId);
                rec.setScore(score);
                rec.setReason("与您学习兴趣相似的同学也在学这门课");
                rec.setRecommendationType("COLLABORATIVE");
                recommendations.add(rec);
            }
        }

        log.info("协同过滤推荐生成：studentId={}, count={}", studentId, recommendations.size());
        return recommendations;
    }

    /**
     * 热门课程推荐
     */
    private List<Recommendation> generatePopularRecommendations(List<Course> candidateCourses) {
        log.debug("执行方法：generatePopularRecommendations, 参数：candidateCount={}", candidateCourses.size());
        List<Recommendation> recommendations = new ArrayList<>();

        // 统计每门课程的选课人数
        Map<Long, Integer> courseEnrollmentCount = new HashMap<>();
        for (Course course : candidateCourses) {
            int count = enrollmentMapper.countByCourseId(course.getCourseId());
            courseEnrollmentCount.put(course.getCourseId(), count);
        }

        // 找出最受欢迎的课程
        int maxCount = courseEnrollmentCount.values().stream().max(Integer::compare).orElse(1);

        for (Course course : candidateCourses) {
            int count = courseEnrollmentCount.getOrDefault(course.getCourseId(), 0);
            double score = (double) count / maxCount * 0.5;

            if (score > 0.2) {
                Recommendation rec = new Recommendation();
                rec.setCourseId(course.getCourseId());
                rec.setScore(score);
                rec.setReason(count + "位同学正在学习，热门课程推荐");
                rec.setRecommendationType("POPULAR");
                recommendations.add(rec);
            }
        }

        // 限制返回数量
        List<Recommendation> result = recommendations.stream()
                .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                .limit(5)
                .collect(Collectors.toList());

        log.info("热门课程推荐生成：count={}", result.size());
        return result;
    }

    /**
     * 查找相似学生（基于选修课程的交集）
     */
    private List<Long> findSimilarStudents(Long studentId, List<Long> enrolledCourseIds) {
        log.debug("执行方法：findSimilarStudents, 参数：studentId={}", studentId);
        // 找到至少有 2 门相同课程的学生
        List<Long> similarStudents = new ArrayList<>();

        List<Long> allStudentIds = studentMapper.findAllStudentIds();

        for (Long otherStudentId : allStudentIds) {
            if (otherStudentId.equals(studentId)) continue;

            List<Long> otherCourses = getEnrolledCourseIds(otherStudentId);

            // 计算课程交集数量
            long intersection = enrolledCourseIds.stream()
                    .filter(otherCourses::contains)
                    .count();

            if (intersection >= 2) {
                similarStudents.add(otherStudentId);
            }
        }

        // 限制返回数量
        List<Long> result = similarStudents.stream().limit(20).collect(Collectors.toList());
        log.info("查找相似学生完成：studentId={}, similarCount={}", studentId, result.size());
        return result;
    }

    /**
     * 获取学生已选修的课程 ID 列表
     */
    private List<Long> getEnrolledCourseIds(Long studentId) {
        List<Long> result = enrollmentMapper.findCourseIdsByStudentId(studentId);
        log.debug("获取学生已选课程：studentId={}, count={}", studentId, result != null ? result.size() : 0);
        return result != null ? result : new ArrayList<>();
    }

    /**
     * 获取已选课程的类别集合
     */
    private Set<String> getEnrolledCategories(List<Long> enrolledCourseIds) {
        Set<String> categories = new HashSet<>();
        for (Long courseId : enrolledCourseIds) {
            Course course = courseMapper.findById(courseId);
            if (course != null && course.getCourseCode() != null) {
                categories.add(course.getCourseCode());
            }
        }
        log.debug("获取已选课程类别：count={}", categories.size());
        return categories;
    }

    /**
     * 将 Date 转换为 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(java.util.Date date) {
        if (date == null) return null;
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Override
    public List<Recommendation> getStudentRecommendations(Long studentId, int limit) {
        log.debug("执行方法：getStudentRecommendations, 参数：studentId={}, limit={}", studentId, limit);
        long startTime = System.currentTimeMillis();
        try {
            // 先尝试获取已有推荐
            List<Recommendation> existingRecommendations = recommendationMapper.findByStudentId(studentId, limit);

            // 如果没有推荐或推荐太少，生成新的
            if (existingRecommendations.isEmpty() || existingRecommendations.size() < limit / 2) {
                log.info("推荐不足，重新生成：studentId={}, existingCount={}", studentId, existingRecommendations.size());
                // 删除旧推荐
                LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
                recommendationMapper.deleteOldRecommendations(studentId, weekAgo);

                // 生成新推荐
                existingRecommendations = generateRecommendations(studentId, limit);
            } else {
                log.info("使用已有推荐：studentId={}, count={}", studentId, existingRecommendations.size());
            }

            List<Recommendation> result = existingRecommendations.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.info("获取学生推荐成功：studentId={}, finalCount={}, 耗时={}ms", studentId, result.size(), duration);
            log.debug("方法返回：result count={}", result.size());
            return result;
        } catch (Exception e) {
            log.error("获取学生推荐失败：studentId={}, limit={}, error={}", studentId, limit, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void markRecommendationAsClicked(Long recommendationId) {
        log.debug("执行方法：markRecommendationAsClicked, 参数：recommendationId={}", recommendationId);
        try {
            recommendationMapper.markAsClicked(recommendationId, LocalDateTime.now());
            log.info("标记推荐为已点击：recommendationId={}", recommendationId);
            log.debug("方法返回：void");
        } catch (Exception e) {
            log.error("标记推荐点击失败：recommendationId={}, error={}", recommendationId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void cleanupOldRecommendations(int days) {
        log.debug("执行方法：cleanupOldRecommendations, 参数：days={}", days);
        long startTime = System.currentTimeMillis();
        try {
            LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
            // 清理所有学生的旧推荐
            List<Long> allStudentIds = studentMapper.findAllStudentIds();
            int totalDeleted = 0;
            for (Long studentId : allStudentIds) {
                int deleted = recommendationMapper.deleteOldRecommendations(studentId, beforeDate);
                totalDeleted += deleted;
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("清理旧推荐完成：studentCount={}, totalDeleted={}, 耗时={}ms", allStudentIds.size(), totalDeleted, duration);
            log.debug("方法返回：void");
        } catch (Exception e) {
            log.error("清理旧推荐失败：days={}, error={}", days, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void refreshRecommendations(Long studentId) {
        log.debug("执行方法：refreshRecommendations, 参数：studentId={}", studentId);
        long startTime = System.currentTimeMillis();
        try {
            // 删除所有旧推荐
            LocalDateTime now = LocalDateTime.now();
            recommendationMapper.deleteOldRecommendations(studentId, now.plusSeconds(1));
            log.info("删除旧推荐：studentId={}", studentId);

            // 生成新推荐
            generateRecommendations(studentId, 10);

            long duration = System.currentTimeMillis() - startTime;
            log.info("刷新推荐成功：studentId={}, 耗时={}ms", studentId, duration);
            log.debug("方法返回：void");
        } catch (Exception e) {
            log.error("刷新推荐失败：studentId={}, error={}", studentId, e.getMessage(), e);
            throw e;
        }
    }
}
