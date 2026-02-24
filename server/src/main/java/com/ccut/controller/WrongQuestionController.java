package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.WrongQuestion;
import com.ccut.service.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 错题本控制器
 * 提供错题的查询、删除、标记掌握等接口
 */
@RestController
@Slf4j
@RequestMapping("/api/wrong-question")
public class WrongQuestionController {

    @Autowired
    private WrongQuestionService wrongQuestionService;

    /**
     * 查询学生的错题列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listWrongQuestions(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        log.debug("收到查询错题列表请求：URI=/api/wrong-question/list, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            if (studentId == null) {
                log.warn("查询错题列表参数错误：studentId 为空");
                return Result.error(400, "studentId 不能为空");
            }
            log.info("执行查询错题列表业务：studentId={}, courseId={}", studentId, courseId);
            List<Map<String, Object>> wrongQuestions = wrongQuestionService.getWrongQuestions(studentId, courseId);
            log.debug("查询错题列表成功：studentId={}, courseId={}, 结果数={}", studentId, courseId, wrongQuestions.size());
            return Result.success(wrongQuestions);
        } catch (Exception e) {
            log.error("查询错题列表失败：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, "查询错题列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询错题详情
     */
    @GetMapping("/detail")
    public Result<WrongQuestion> getWrongQuestionDetail(@RequestParam("id") Long id) {
        log.debug("收到查询错题详情请求：URI=/api/wrong-question/detail, 参数：id={}", id, id);
        try {
            if (id == null) {
                log.warn("查询错题详情参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            log.info("执行查询错题详情业务：id={}", id);
            WrongQuestion wrongQuestion = wrongQuestionService.getWrongQuestionById(id);
            if (wrongQuestion == null) {
                log.warn("错题不存在：id={}", id);
                return Result.error(404, "错题不存在");
            }
            log.debug("查询错题详情成功：id={}", id);
            return Result.success(wrongQuestion);
        } catch (Exception e) {
            log.error("查询错题详情失败：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, "查询错题详情失败：" + e.getMessage());
        }
    }

    /**
     * 复习错题
     */
    @PostMapping("/review")
    public Result<Map<String, Object>> reviewWrongQuestion(
            @RequestParam("id") Long id,
            @RequestParam("isCorrect") Boolean isCorrect) {
        log.debug("收到复习错题请求：URI=/api/wrong-question/review, 参数：id={}, isCorrect={}", id, isCorrect);
        try {
            if (id == null) {
                log.warn("复习错题参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            if (isCorrect == null) {
                log.warn("复习错题参数错误：isCorrect 为空");
                return Result.error(400, "isCorrect 不能为空");
            }

            log.info("执行复习错题业务：id={}, isCorrect={}", id, isCorrect);
            boolean isDeleted = wrongQuestionService.reviewWrongQuestion(id, isCorrect);

            Map<String, Object> result = new HashMap<>();
            result.put("isDeleted", isDeleted);
            result.put("message", isDeleted ? "恭喜！连续答对 3 次，错题已自动移除" :
                    isCorrect ? "答对了！继续加油" : "答错了，已重置计数");

            log.debug("复习错题成功：id={}, isCorrect={}, isDeleted={}", id, isCorrect, isDeleted);
            return Result.success(result);
        } catch (Exception e) {
            log.error("复习错题失败：id={}, isCorrect={}, 错误：{}", id, isCorrect, e.getMessage(), e);
            return Result.error(500, "复习错题失败：" + e.getMessage());
        }
    }

    /**
     * 获取错题统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        log.debug("收到获取错题统计信息请求：URI=/api/wrong-question/statistics, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            if (studentId == null) {
                log.warn("获取错题统计信息参数错误：studentId 为空");
                return Result.error(400, "studentId 不能为空");
            }
            log.info("执行获取错题统计信息业务：studentId={}, courseId={}", studentId, courseId);
            Map<String, Object> statistics = wrongQuestionService.getStatistics(studentId, courseId);
            log.debug("获取错题统计信息成功：studentId={}, courseId={}", studentId, courseId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取错题统计信息失败：studentId={}, courseId={}, 错误：{}", studentId, courseId, e.getMessage(), e);
            return Result.error(500, "获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 标记错题为已掌握
     */
    @PostMapping("/mark-mastered")
    public Result<String> markAsMastered(@RequestParam("id") Long id) {
        log.debug("收到标记错题为已掌握请求：URI=/api/wrong-question/mark-mastered, 参数：id={}", id);
        try {
            if (id == null) {
                log.warn("标记错题为已掌握参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            log.info("执行标记错题为已掌握业务：id={}", id);
            wrongQuestionService.markAsMastered(id);
            log.debug("标记错题为已掌握成功：id={}", id);
            return Result.success("标记成功");
        } catch (Exception e) {
            log.error("标记错题为已掌握失败：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, "标记掌握失败：" + e.getMessage());
        }
    }

    /**
     * 取消错题的掌握标记
     */
    @PostMapping("/cancel-mastered")
    public Result<String> cancelMastered(@RequestParam("id") Long id) {
        log.debug("收到取消错题掌握标记请求：URI=/api/wrong-question/cancel-mastered, 参数：id={}", id);
        try {
            if (id == null) {
                log.warn("取消错题掌握标记参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            log.info("执行取消错题掌握标记业务：id={}", id);
            wrongQuestionService.cancelMastered(id);
            log.debug("取消错题掌握标记成功：id={}", id);
            return Result.success("取消标记成功");
        } catch (Exception e) {
            log.error("取消错题掌握标记失败：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, "取消掌握标记失败：" + e.getMessage());
        }
    }

    /**
     * 删除错题
     */
    @DeleteMapping("/delete")
    public Result<String> deleteWrongQuestion(@RequestParam("id") Long id) {
        log.debug("收到删除错题请求：URI=/api/wrong-question/delete, 参数：id={}", id);
        try {
            if (id == null) {
                log.warn("删除错题参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            log.info("执行删除错题业务：id={}", id);
            wrongQuestionService.deleteWrongQuestion(id);
            log.debug("删除错题成功：id={}", id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除错题失败：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, "删除错题失败：" + e.getMessage());
        }
    }

    /**
     * 批量标记错题为已掌握
     */
    @PostMapping("/batch-mark-mastered")
    public Result<String> batchMarkAsMastered(@RequestBody List<Long> ids) {
        log.debug("收到批量标记错题为已掌握请求：URI=/api/wrong-question/batch-mark-mastered, 参数：ids 大小={}", ids != null ? ids.size() : 0);
        try {
            if (ids == null || ids.isEmpty()) {
                log.warn("批量标记错题为已掌握参数错误：ids 为空");
                return Result.error(400, "ids 不能为空");
            }
            log.info("执行批量标记错题为已掌握业务：ids 大小={}", ids.size());
            for (Long id : ids) {
                wrongQuestionService.markAsMastered(id);
            }
            log.debug("批量标记错题为已掌握成功：ids 大小={}", ids.size());
            return Result.success("批量标记成功，共标记 " + ids.size() + " 道错题");
        } catch (Exception e) {
            log.error("批量标记错题为已掌握失败：ids={}, 错误：{}", ids, e.getMessage(), e);
            return Result.error(500, "批量标记失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除错题
     */
    @PostMapping("/batch-delete")
    public Result<String> batchDeleteWrongQuestions(@RequestBody List<Long> ids) {
        log.debug("收到批量删除错题请求：URI=/api/wrong-question/batch-delete, 参数：ids 大小={}", ids != null ? ids.size() : 0);
        try {
            if (ids == null || ids.isEmpty()) {
                log.warn("批量删除错题参数错误：ids 为空");
                return Result.error(400, "ids 不能为空");
            }
            log.info("执行批量删除错题业务：ids 大小={}", ids.size());
            for (Long id : ids) {
                wrongQuestionService.deleteWrongQuestion(id);
            }
            log.debug("批量删除错题成功：ids 大小={}", ids.size());
            return Result.success("批量删除成功，共删除 " + ids.size() + " 道错题");
        } catch (Exception e) {
            log.error("批量删除错题失败：ids={}, 错误：{}", ids, e.getMessage(), e);
            return Result.error(500, "批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 更新错题笔记
     */
    @PostMapping("/update-note")
    public Result<String> updateNote(
            @RequestParam("id") Long id,
            @RequestBody String note) {
        log.debug("收到更新错题笔记请求：URI=/api/wrong-question/update-note, 参数：id={}, note 长度={}", id, note != null ? note.length() : 0);
        try {
            if (id == null) {
                log.warn("更新错题笔记参数错误：id 为空");
                return Result.error(400, "id 不能为空");
            }
            log.info("执行更新错题笔记业务：id={}", id);
            wrongQuestionService.updateNote(id, note);
            log.debug("更新错题笔记成功：id={}", id);
            return Result.success("笔记更新成功");
        } catch (Exception e) {
            log.error("更新错题笔记失败：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, "更新笔记失败：" + e.getMessage());
        }
    }

    /**
     * 添加错题到错题本
     */
    @PostMapping("/add")
    public Result<String> addToWrongBook(
            @RequestParam("studentId") Long studentId,
            @RequestParam("questionId") Long questionId,
            @RequestParam("examId") Long examId,
            @RequestParam("courseId") Long courseId,
            @RequestParam("wrongAnswer") String wrongAnswer,
            @RequestParam("correctAnswer") String correctAnswer) {
        log.debug("收到添加错题请求：URI=/api/wrong-question/add, 参数：studentId={}, questionId={}, examId={}, courseId={}",
                studentId, questionId, examId, courseId);
        try {
            if (studentId == null) {
                log.warn("添加错题参数错误：studentId 为空");
                return Result.error(400, "studentId 不能为空");
            }
            if (questionId == null) {
                log.warn("添加错题参数错误：questionId 为空");
                return Result.error(400, "questionId 不能为空");
            }
            if (examId == null) {
                log.warn("添加错题参数错误：examId 为空");
                return Result.error(400, "examId 不能为空");
            }
            if (courseId == null) {
                log.warn("添加错题参数错误：courseId 为空");
                return Result.error(400, "courseId 不能为空");
            }

            log.info("执行添加错题业务：studentId={}, questionId={}, examId={}, courseId={}",
                    studentId, questionId, examId, courseId);
            wrongQuestionService.addToWrongBook(studentId, questionId, examId, courseId, wrongAnswer, correctAnswer);
            log.debug("添加错题成功：studentId={}, questionId={}", studentId, questionId);
            return Result.success("错题添加成功");
        } catch (Exception e) {
            log.error("添加错题失败：studentId={}, questionId={}, examId={}, courseId={}, 错误：{}",
                    studentId, questionId, examId, courseId, e.getMessage(), e);
            return Result.error(500, "添加错题失败：" + e.getMessage());
        }
    }
}
