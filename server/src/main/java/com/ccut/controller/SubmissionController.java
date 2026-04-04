package com.ccut.controller;

import com.ccut.dto.PersonalSubmissionDTO;
import com.ccut.dto.Result;
import com.ccut.entity.StudentSubmission;
import com.ccut.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作业提交控制器
 */
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
        List<StudentSubmission> submissions = submissionService.selectAll();
        return Result.success(submissions);
    }

    /**
     * 教师：根据 assignmentId 查看提交
     */
    @GetMapping("/{assignmentId}")
    public Result<List<StudentSubmission>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        List<StudentSubmission> submissions = submissionService.selectByAssignmentId(assignmentId);
        return Result.success(submissions);
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
        StudentSubmission submission = submissionService.uploadSubmission(assignmentId, groupId, studentId, submissionContent, files);
        return Result.success(submission);
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
        String result = submissionService.updateSubmission(submissionId, submissionContent, files);
        return Result.success(result);
    }

    /**
     * 教师：添加或更新小组整体评语
     */
    @PutMapping("/{submissionId}/comment")
    public Result<String> updateGroupComment(
            @PathVariable Long submissionId,
            @RequestBody Map<String, String> body
    ) {
        String groupComment = body.get("groupComment");
        String result = submissionService.updateGroupComment(submissionId, groupComment);
        return Result.success(result);
    }

    /**
     * 学生/教师：获取提交详情
     */
    @GetMapping("/detail/{submissionId}")
    public Result<StudentSubmission> getSubmissionDetail(@PathVariable Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("submissionId 不能为空");
        }
        StudentSubmission submission = submissionService.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("未找到该提交记录");
        }
        return Result.success(submission);
    }

    /**
     * 教师：获取小组整体评语
     */
    @GetMapping("/{submissionId}/comment")
    public Result<Map<String, String>> getGroupComment(@PathVariable Long submissionId) {
        Map<String, String> comment = submissionService.getGroupComment(submissionId);
        return Result.success(comment);
    }

    /**
     * 学生：查询自己小组的所有提交记录
     */
    @GetMapping("/my-group")
    public Result<List<StudentSubmission>> getMyGroupSubmissions(@RequestParam Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId 不能为空");
        }
        List<StudentSubmission> submissions = submissionService.listByGroupId(groupId);
        return Result.success(submissions != null ? submissions : List.of());
    }

    /**
     * 教师：查看某个作业的所有个人提交记录（按提交人）
     */
    @GetMapping("/personal-submission/by-assignment")
    public Result<List<PersonalSubmissionDTO>> getPersonalSubmissionsByAssignment(@RequestParam Long assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("assignmentId 不能为空");
        }
        List<StudentSubmission> submissions = submissionService.selectByAssignmentId(assignmentId);

        // 转换为前端期望的 DTO 格式
        List<PersonalSubmissionDTO> dtoList = submissions.stream()
                .map(sub -> {
                    PersonalSubmissionDTO dto = new PersonalSubmissionDTO();
                    dto.setStudentId(sub.getSubmittedBy());
                    dto.setSubmittedAt(sub.getSubmittedAt());
                    dto.setStatus(sub.getStatus() != null ? sub.getStatus().toString() : null);
                    dto.setScore(null);
                    dto.setSubmissionContent(sub.getSubmissionContent());
                    dto.setSubmissionFiles(sub.getSubmissionFiles());
                    return dto;
                })
                .collect(Collectors.toList());

        return Result.success(dtoList);
    }
}
