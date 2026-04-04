package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.WrongQuestion;
import com.ccut.service.WrongQuestionService;
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
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        List<Map<String, Object>> wrongQuestions = wrongQuestionService.getWrongQuestions(studentId, courseId);
        return Result.success(wrongQuestions);
    }

    /**
     * 查询错题详情
     */
    @GetMapping("/detail")
    public Result<WrongQuestion> getWrongQuestionDetail(@RequestParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        WrongQuestion wrongQuestion = wrongQuestionService.getWrongQuestionById(id);
        if (wrongQuestion == null) {
            throw new RuntimeException("错题不存在");
        }
        return Result.success(wrongQuestion);
    }

    /**
     * 复习错题
     */
    @PostMapping("/review")
    public Result<Map<String, Object>> reviewWrongQuestion(
            @RequestParam("id") Long id,
            @RequestParam("isCorrect") Boolean isCorrect) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        if (isCorrect == null) {
            throw new IllegalArgumentException("isCorrect 不能为空");
        }

        boolean isDeleted = wrongQuestionService.reviewWrongQuestion(id, isCorrect);

        Map<String, Object> result = new HashMap<>();
        result.put("isDeleted", isDeleted);
        result.put("message", isDeleted ? "恭喜！连续答对 3 次，错题已自动移除" :
                isCorrect ? "答对了！继续加油" : "答错了，已重置计数");

        return Result.success(result);
    }

    /**
     * 获取错题统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        Map<String, Object> statistics = wrongQuestionService.getStatistics(studentId, courseId);
        return Result.success(statistics);
    }

    /**
     * 标记错题为已掌握
     */
    @PostMapping("/mark-mastered")
    public Result<String> markAsMastered(@RequestParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        wrongQuestionService.markAsMastered(id);
        return Result.success("标记成功");
    }

    /**
     * 取消错题的掌握标记
     */
    @PostMapping("/cancel-mastered")
    public Result<String> cancelMastered(@RequestParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        wrongQuestionService.cancelMastered(id);
        return Result.success("取消标记成功");
    }

    /**
     * 删除错题
     */
    @DeleteMapping("/delete")
    public Result<String> deleteWrongQuestion(@RequestParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        wrongQuestionService.deleteWrongQuestion(id);
        return Result.success("删除成功");
    }

    /**
     * 批量标记错题为已掌握
     */
    @PostMapping("/batch-mark-mastered")
    public Result<String> batchMarkAsMastered(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids 不能为空");
        }
        for (Long id : ids) {
            wrongQuestionService.markAsMastered(id);
        }
        return Result.success("批量标记成功，共标记 " + ids.size() + " 道错题");
    }

    /**
     * 批量删除错题
     */
    @PostMapping("/batch-delete")
    public Result<String> batchDeleteWrongQuestions(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids 不能为空");
        }
        for (Long id : ids) {
            wrongQuestionService.deleteWrongQuestion(id);
        }
        return Result.success("批量删除成功，共删除 " + ids.size() + " 道错题");
    }

    /**
     * 更新错题笔记
     */
    @PostMapping("/update-note")
    public Result<String> updateNote(
            @RequestParam("id") Long id,
            @RequestBody String note) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        wrongQuestionService.updateNote(id, note);
        return Result.success("笔记更新成功");
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
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        if (questionId == null) {
            throw new IllegalArgumentException("questionId 不能为空");
        }
        if (examId == null) {
            throw new IllegalArgumentException("examId 不能为空");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("courseId 不能为空");
        }

        wrongQuestionService.addToWrongBook(studentId, questionId, examId, courseId, wrongAnswer, correctAnswer);
        return Result.success("错题添加成功");
    }
}
