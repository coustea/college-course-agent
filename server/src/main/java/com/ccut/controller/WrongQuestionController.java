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
     *
     * @param studentId 学生ID
     * @param courseId  课程ID（可选，不传则查询所有课程的错题）
     * @return 错题列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listWrongQuestions(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        try {
            if (studentId == null) {
                return Result.error(400, "studentId 不能为空");
            }
            List<Map<String, Object>> wrongQuestions = wrongQuestionService.getWrongQuestions(studentId, courseId);
            return Result.success(wrongQuestions);
        } catch (Exception e) {
            log.error("查询错题列表失败: studentId={}, courseId={}", studentId, courseId, e);
            return Result.error(500, "查询错题列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询错题详情
     *
     * @param id 错题ID
     * @return 错题详情
     */
    @GetMapping("/detail")
    public Result<WrongQuestion> getWrongQuestionDetail(@RequestParam("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            WrongQuestion wrongQuestion = wrongQuestionService.getWrongQuestionById(id);
            if (wrongQuestion == null) {
                return Result.error(404, "错题不存在");
            }
            return Result.success(wrongQuestion);
        } catch (Exception e) {
            log.error("查询错题详情失败: id={}", id, e);
            return Result.error(500, "查询错题详情失败: " + e.getMessage());
        }
    }

    /**
     * 复习错题
     * 答对：连续答对次数+1，达到3次自动删除错题
     * 答错：重置连续答对次数为0，并累加错误次数
     *
     * @param id        错题ID
     * @param isCorrect 是否答对
     * @return 复习结果（是否已删除）
     */
    @PostMapping("/review")
    public Result<Map<String, Object>> reviewWrongQuestion(
            @RequestParam("id") Long id,
            @RequestParam("isCorrect") Boolean isCorrect) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            if (isCorrect == null) {
                return Result.error(400, "isCorrect 不能为空");
            }

            boolean isDeleted = wrongQuestionService.reviewWrongQuestion(id, isCorrect);

            Map<String, Object> result = new HashMap<>();
            result.put("isDeleted", isDeleted);
            result.put("message", isDeleted ? "恭喜！连续答对3次，错题已自动移除" :
                    isCorrect ? "答对了！继续加油" : "答错了，已重置计数");

            return Result.success(result);
        } catch (Exception e) {
            log.error("复习错题失败: id={}, isCorrect={}", id, isCorrect, e);
            return Result.error(500, "复习错题失败: " + e.getMessage());
        }
    }

    /**
     * 获取错题统计信息
     *
     * @param studentId 学生ID
     * @param courseId  课程ID（可选）
     * @return 统计信息（total, mastered, unmastered, masteredRate）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        try {
            if (studentId == null) {
                return Result.error(400, "studentId 不能为空");
            }
            Map<String, Object> statistics = wrongQuestionService.getStatistics(studentId, courseId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取错题统计信息失败: studentId={}, courseId={}", studentId, courseId, e);
            return Result.error(500, "获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 标记错题为已掌握
     *
     * @param id 错题ID
     * @return 操作结果
     */
    @PostMapping("/mark-mastered")
    public Result<String> markAsMastered(@RequestParam("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            wrongQuestionService.markAsMastered(id);
            return Result.success("标记成功");
        } catch (Exception e) {
            log.error("标记错题为已掌握失败: id={}", id, e);
            return Result.error(500, "标记掌握失败: " + e.getMessage());
        }
    }

    /**
     * 取消错题的掌握标记
     *
     * @param id 错题ID
     * @return 操作结果
     */
    @PostMapping("/cancel-mastered")
    public Result<String> cancelMastered(@RequestParam("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            wrongQuestionService.cancelMastered(id);
            return Result.success("取消标记成功");
        } catch (Exception e) {
            log.error("取消错题掌握标记失败: id={}", id, e);
            return Result.error(500, "取消掌握标记失败: " + e.getMessage());
        }
    }

    /**
     * 删除错题
     *
     * @param id 错题ID
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public Result<String> deleteWrongQuestion(@RequestParam("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            wrongQuestionService.deleteWrongQuestion(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除错题失败: id={}", id, e);
            return Result.error(500, "删除错题失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记错题为已掌握
     *
     * @param ids 错题ID列表
     * @return 操作结果
     */
    @PostMapping("/batch-mark-mastered")
    public Result<String> batchMarkAsMastered(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error(400, "ids 不能为空");
            }
            for (Long id : ids) {
                wrongQuestionService.markAsMastered(id);
            }
            return Result.success("批量标记成功，共标记 " + ids.size() + " 道错题");
        } catch (Exception e) {
            log.error("批量标记错题为已掌握失败: ids={}", ids, e);
            return Result.error(500, "批量标记失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除错题
     *
     * @param ids 错题ID列表
     * @return 操作结果
     */
    @PostMapping("/batch-delete")
    public Result<String> batchDeleteWrongQuestions(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error(400, "ids 不能为空");
            }
            for (Long id : ids) {
                wrongQuestionService.deleteWrongQuestion(id);
            }
            return Result.success("批量删除成功，共删除 " + ids.size() + " 道错题");
        } catch (Exception e) {
            log.error("批量删除错题失败: ids={}", ids, e);
            return Result.error(500, "批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新错题笔记
     *
     * @param id   错题ID
     * @param note 笔记内容
     * @return 操作结果
     */
    @PostMapping("/update-note")
    public Result<String> updateNote(
            @RequestParam("id") Long id,
            @RequestBody String note) {
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            wrongQuestionService.updateNote(id, note);
            return Result.success("笔记更新成功");
        } catch (Exception e) {
            log.error("更新错题笔记失败: id={}", id, e);
            return Result.error(500, "更新笔记失败: " + e.getMessage());
        }
    }
}
