package com.ccut.service;

import com.ccut.dto.AiExamGenerateRequest;
import com.ccut.dto.AiExamSubmitRequest;
import com.ccut.entity.AiExam;

import java.util.List;
import java.util.Map;

/**
 * AI考试服务接口
 */
public interface AiExamService {

    /**
     * 生成AI考试
     * @param req 生成请求参数
     * @return 包含考试和题目的响应数据
     */
    Map<String, Object> generateExam(AiExamGenerateRequest req);

    /**
     * 提交考试答案
     * @param req 提交请求参数
     * @return 包含尝试记录和答案的响应数据
     */
    Map<String, Object> submitExam(AiExamSubmitRequest req);

    /**
     * 查询某学生在某课程的题目正确率（0~1 小数）
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 包含正确率数据的响应
     */
    Map<String, Object> getAccuracy(Long studentId, Long courseId);

    /**
     * 获取学生在某课程中题目数为5的考试的平均成绩
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 包含平均成绩的响应
     */
    Map<String, Object> getAverageScore(Long studentId, Long courseId);

    /**
     * 获取学生在某课程中每个视频和文档的详细成绩
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 包含视频和文档成绩的响应
     */
    Map<String, Object> getDetailedScores(Long studentId, Long courseId);

    /**
     * 获取学生在某课程中的所有考试记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 考试记录列表
     */
    List<AiExam> listExams(Long studentId, Long courseId);

}
