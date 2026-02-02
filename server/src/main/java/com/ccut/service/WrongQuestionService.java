package com.ccut.service;

import com.ccut.entity.WrongQuestion;

import java.util.List;
import java.util.Map;

/**
 * 错题本服务接口
 */
public interface WrongQuestionService {
    void updateNote(Long id, String note);

    /**
     * 自动添加错题到错题本
     * 如果题目已存在，则更新错误次数；否则新增记录
     *
     * @param studentId    学生ID
     * @param questionId   题目ID
     * @param examId       考试ID
     * @param courseId     课程ID
     * @param wrongAnswer  学生的错误答案
     * @param correctAnswer 正确答案
     */
    void addToWrongBook(Long studentId, Long questionId, Long examId, Long courseId,
                        String wrongAnswer, String correctAnswer);

    /**
     * 复习错题
     * 如果答对：连续答对次数+1，如果达到3次则自动删除错题
     * 如果答错：重置连续答对次数为0，并累加错误次数
     *
     * @param wrongQuestionId 错题ID
     * @param isCorrect      是否答对
     * @return 是否已删除（连续答对3次自动删除）
     */
    boolean reviewWrongQuestion(Long wrongQuestionId, boolean isCorrect);

    /**
     * 查询学生的错题列表（可按课程ID筛选）
     *
     * @param studentId 学生ID
     * @param courseId  课程ID（可选）
     * @return 错题列表（包含题目详情）
     */
    List<Map<String, Object>> getWrongQuestions(Long studentId, Long courseId);

    /**
     * 查询错题详情
     *
     * @param id 错题ID
     * @return 错题详情
     */
    WrongQuestion getWrongQuestionById(Long id);

    /**
     * 标记错题为已掌握
     *
     * @param id 错题ID
     */
    void markAsMastered(Long id);

    /**
     * 取消错题的掌握标记
     *
     * @param id 错题ID
     */
    void cancelMastered(Long id);

    /**
     * 删除错题
     *
     * @param id 错题ID
     */
    void deleteWrongQuestion(Long id);

    /**
     * 获取错题统计信息
     *
     * @param studentId 学生ID
     * @param courseId  课程ID（可选）
     * @return 统计信息（total, mastered, unmastered）
     */
    Map<String, Object> getStatistics(Long studentId, Long courseId);
}
