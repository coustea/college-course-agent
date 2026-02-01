package com.ccut.service.Impl;

import com.ccut.entity.WrongQuestion;
import com.ccut.mapper.WrongQuestionMapper;
import com.ccut.service.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 错题本服务实现类
 */
@Service
@Slf4j
public class WrongQuestionServiceImpl implements WrongQuestionService {

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Override
    @Transactional
    public void addToWrongBook(Long studentId, Long questionId, Long examId, Long courseId,
                                String wrongAnswer, String correctAnswer) {
        try {
            // 查询是否已存在该错题记录
            WrongQuestion existing = wrongQuestionMapper.selectByStudentIdAndQuestionId(studentId, questionId);

            if (existing != null) {
                // 如果已存在，更新错误次数和时间
                // 如果之前已掌握，现在又答错了，需要取消掌握标记
                wrongQuestionMapper.updateWrongCount(existing.getId());
                log.info("更新错题记录: studentId={}, questionId={}, wrongCount={}",
                        studentId, questionId, existing.getWrongCount() + 1);
            } else {
                // 如果不存在，新增错题记录
                WrongQuestion wrongQuestion = new WrongQuestion();
                wrongQuestion.setStudentId(studentId);
                wrongQuestion.setQuestionId(questionId);
                wrongQuestion.setExamId(examId);
                wrongQuestion.setCourseId(courseId);
                wrongQuestion.setWrongAnswer(wrongAnswer);
                wrongQuestion.setCorrectAnswer(correctAnswer);
                wrongQuestion.setWrongCount(1);
                wrongQuestion.setCorrectCount(0);  // 初始答对次数为0
                wrongQuestion.setIsMastered(false);
                wrongQuestion.setFirstWrongTime(new Date());
                wrongQuestion.setLastWrongTime(new Date());
                wrongQuestion.setCreateTime(new Date());
                wrongQuestion.setUpdateTime(new Date());

                wrongQuestionMapper.insert(wrongQuestion);
                log.info("新增错题记录: studentId={}, questionId={}, examId={}",
                        studentId, questionId, examId);
            }
        } catch (Exception e) {
            log.error("添加错题到错题本失败: studentId={}, questionId={}", studentId, questionId, e);
            throw new RuntimeException("添加错题失败", e);
        }
    }

    @Override
    @Transactional
    public boolean reviewWrongQuestion(Long wrongQuestionId, boolean isCorrect) {
        try {
            // 查询错题详情
            WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(wrongQuestionId);
            if (wrongQuestion == null) {
                throw new IllegalArgumentException("错题不存在");
            }

            if (isCorrect) {
                // 答对了：增加连续答对次数
                wrongQuestionMapper.incrementCorrectCount(wrongQuestionId);
                int newCorrectCount = wrongQuestion.getCorrectCount() + 1;
                log.info("复习答对: wrongQuestionId={}, correctCount={}", wrongQuestionId, newCorrectCount);

                // 如果连续答对3次，自动删除错题
                if (newCorrectCount >= 3) {
                    wrongQuestionMapper.deleteById(wrongQuestionId);
                    log.info("连续答对3次，自动删除错题: wrongQuestionId={}", wrongQuestionId);
                    return true;  // 返回true表示已删除
                }
                return false;  // 未删除
            } else {
                // 答错了：重置连续答对次数为0，并累加错误次数
                wrongQuestionMapper.resetCorrectCount(wrongQuestionId);
                wrongQuestionMapper.updateWrongCount(wrongQuestionId);
                log.info("复习答错: wrongQuestionId={}, correctCount重置为0, wrongCount+1",
                        wrongQuestionId);
                return false;  // 未删除
            }
        } catch (Exception e) {
            log.error("复习错题失败: wrongQuestionId={}, isCorrect={}", wrongQuestionId, isCorrect, e);
            throw new RuntimeException("复习错题失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> getWrongQuestions(Long studentId, Long courseId) {
        try {
            List<WrongQuestion> wrongQuestions = wrongQuestionMapper.selectDetailByStudentId(studentId, courseId);
            List<Map<String, Object>> result = new ArrayList<>();

            for (WrongQuestion wq : wrongQuestions) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", wq.getId());
                item.put("studentId", wq.getStudentId());
                item.put("questionId", wq.getQuestionId());
                item.put("examId", wq.getExamId());
                item.put("courseId", wq.getCourseId());
                item.put("wrongAnswer", wq.getWrongAnswer());
                item.put("correctAnswer", wq.getCorrectAnswer());
                item.put("wrongCount", wq.getWrongCount());
                item.put("correctCount", wq.getCorrectCount());  // 添加连续答对次数
                item.put("isMastered", wq.getIsMastered());
                item.put("firstWrongTime", wq.getFirstWrongTime());
                item.put("lastWrongTime", wq.getLastWrongTime());
                item.put("masteredTime", wq.getMasteredTime());

                // 题目详细信息
                if (wq.getQuestion() != null) {
                    Map<String, Object> question = new HashMap<>();
                    question.put("id", wq.getQuestion().getId());
                    question.put("type", wq.getQuestion().getType());
                    question.put("content", wq.getQuestion().getContent());
                    question.put("options", wq.getQuestion().getOptions());
                    question.put("analysis", wq.getQuestion().getAnalysis());
                    item.put("question", question);
                }

                result.add(item);
            }

            return result;
        } catch (Exception e) {
            log.error("查询错题列表失败: studentId={}, courseId={}", studentId, courseId, e);
            throw new RuntimeException("查询错题列表失败", e);
        }
    }

    @Override
    public WrongQuestion getWrongQuestionById(Long id) {
        try {
            return wrongQuestionMapper.selectById(id);
        } catch (Exception e) {
            log.error("查询错题详情失败: id={}", id, e);
            throw new RuntimeException("查询错题详情失败", e);
        }
    }

    @Override
    @Transactional
    public void markAsMastered(Long id) {
        try {
            wrongQuestionMapper.markAsMastered(id);
            log.info("标记错题为已掌握: id={}", id);
        } catch (Exception e) {
            log.error("标记错题为已掌握失败: id={}", id, e);
            throw new RuntimeException("标记掌握失败", e);
        }
    }

    @Override
    @Transactional
    public void cancelMastered(Long id) {
        try {
            wrongQuestionMapper.cancelMastered(id);
            log.info("取消错题掌握标记: id={}", id);
        } catch (Exception e) {
            log.error("取消错题掌握标记失败: id={}", id, e);
            throw new RuntimeException("取消掌握标记失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteWrongQuestion(Long id) {
        try {
            wrongQuestionMapper.deleteById(id);
            log.info("删除错题记录: id={}", id);
        } catch (Exception e) {
            log.error("删除错题记录失败: id={}", id, e);
            throw new RuntimeException("删除错题失败", e);
        }
    }

    @Override
    public Map<String, Object> getStatistics(Long studentId, Long courseId) {
        try {
            int total = wrongQuestionMapper.countByStudentId(studentId, courseId);
            int mastered = wrongQuestionMapper.countMasteredByStudentId(studentId, courseId);
            int unmastered = total - mastered;

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("mastered", mastered);
            stats.put("unmastered", unmastered);
            stats.put("masteredRate", total > 0 ? (double) mastered / total : 0.0);

            return stats;
        } catch (Exception e) {
            log.error("获取错题统计信息失败: studentId={}, courseId={}", studentId, courseId, e);
            throw new RuntimeException("获取统计信息失败", e);
        }
    }
}
