package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.StudentSubmission;
import com.ccut.service.SubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 作业提交控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/submission")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    /**
     * 教师：查看所有学生提交的作业
     */
    @GetMapping
    public Result<List<StudentSubmission>> selectAll() {
        try {
            return Result.success(submissionService.selectAll());
        } catch (Exception e) {
            log.error("查询所有作业提交失败: {}", e.getMessage(), e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 教师：根据 assignmentId 查看提交
     */
    @GetMapping("/{assignmentId}")
    public Result<List<StudentSubmission>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        try {
            return Result.success(submissionService.selectByAssignmentId(assignmentId));
        } catch (Exception e) {
            log.error("查询作业提交失败 assignmentId={}: {}", assignmentId, e.getMessage(), e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 学生：上传作业
     */
    @PostMapping("/upload")
    public Result<StudentSubmission> upload(
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("groupId") Long groupId,
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "content", required = false) String submissionContent,
            @RequestParam("files") List<MultipartFile> files
    ) {
        try {
            return Result.success(submissionService.uploadSubmission(assignmentId, groupId, studentId, submissionContent, files));
        } catch (IllegalArgumentException e) {
            log.warn("上传作业参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("上传作业业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 学生：修改已提交的作业（允许更新文本与附件）
     */
    @PutMapping("/{submissionId}")
    public Result<String> updateSubmission(
            @PathVariable Long submissionId,
            @RequestParam(value = "content", required = false) String submissionContent,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            return Result.success(submissionService.updateSubmission(submissionId, submissionContent, files));
        } catch (RuntimeException e) {
            log.warn("更新提交业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新提交失败: {}", e.getMessage(), e);
            return Result.error(500, "更新提交失败: " + e.getMessage());
        }
    }

    /**
     * 教师：添加或更新小组整体评语
     */
    @PutMapping("/{submissionId}/comment")
    public Result<String> updateGroupComment(
            @PathVariable Long submissionId,
            @RequestBody Map<String, String> body
    ) {
        try {
            String groupComment = body.get("groupComment");
            return Result.success(submissionService.updateGroupComment(submissionId, groupComment));
        } catch (IllegalArgumentException e) {
            log.warn("更新小组评语参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新小组评语业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新小组评语失败: submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "更新小组评语失败: " + e.getMessage());
        }
    }

    /**
     * 学生/教师：获取提交详情
     */
    @GetMapping("/detail/{submissionId}")
    public Result<StudentSubmission> getSubmissionDetail(@PathVariable Long submissionId) {
        try {
            if (submissionId == null) {
                return Result.error(400, "submissionId 不能为空");
            }

            StudentSubmission submission = submissionService.selectById(submissionId);
            if (submission == null) {
                return Result.error(404, "未找到该提交记录");
            }

            log.info("获取提交详情: submissionId={}", submissionId);
            return Result.success(submission);
        } catch (Exception e) {
            log.error("获取提交详情失败: submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "获取提交详情失败: " + e.getMessage());
        }
    }

    /**
     * 教师：获取小组整体评语
     */
    @GetMapping("/{submissionId}/comment")
    public Result<Map<String, String>> getGroupComment(@PathVariable Long submissionId) {
        try {
            return Result.success(submissionService.getGroupComment(submissionId));
        } catch (RuntimeException e) {
            log.warn("获取小组评语业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("获取小组评语失败: submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "获取小组评语失败: " + e.getMessage());
        }
    }

    /**
     * 学生：查询自己小组的所有提交记录
     */
    @GetMapping("/my-group")
    public Result<List<StudentSubmission>> getMyGroupSubmissions(@RequestParam Long groupId) {
        try {
            if (groupId == null) {
                return Result.error(400, "groupId 不能为空");
            }

            List<StudentSubmission> submissions = submissionService.listByGroupId(groupId);

            log.info("查询小组提交记录: groupId={}, 找到{}条记录", groupId, submissions != null ? submissions.size() : 0);
            return Result.success(submissions != null ? submissions : List.of());
        } catch (Exception e) {
            log.error("查询小组提交记录失败: groupId={}, error={}", groupId, e.getMessage(), e);
            return Result.error(500, "查询小组提交记录失败: " + e.getMessage());
        }
    }

}
