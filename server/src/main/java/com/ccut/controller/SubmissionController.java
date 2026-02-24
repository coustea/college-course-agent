package com.ccut.controller;

import com.ccut.dto.PersonalSubmissionDTO;
import com.ccut.dto.Result;
import com.ccut.entity.StudentSubmission;
import com.ccut.service.SubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        log.debug("收到查询所有作业提交请求：URI=/api/submission");
        try {
            log.info("执行查询所有作业提交业务");
            List<StudentSubmission> submissions = submissionService.selectAll();
            log.debug("查询所有作业提交成功：结果数={}", submissions.size());
            return Result.success(submissions);
        } catch (Exception e) {
            log.error("查询所有作业提交失败：错误：{}", e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 教师：根据 assignmentId 查看提交
     */
    @GetMapping("/{assignmentId}")
    public Result<List<StudentSubmission>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        log.debug("收到查询作业提交请求：URI=/api/submission/{}, 参数：assignmentId={}", assignmentId, assignmentId);
        try {
            log.info("执行查询作业提交业务：assignmentId={}", assignmentId);
            List<StudentSubmission> submissions = submissionService.selectByAssignmentId(assignmentId);
            log.debug("查询作业提交成功：assignmentId={}, 结果数={}", assignmentId, submissions.size());
            return Result.success(submissions);
        } catch (Exception e) {
            log.error("查询作业提交失败 assignmentId={}: {}", assignmentId, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
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
        log.debug("收到上传作业请求：URI=/api/submission/upload, 参数：assignmentId={}, groupId={}, studentId={}, 文件数={}",
                assignmentId, groupId, studentId, files != null ? files.size() : 0);
        try {
            long totalFileSize = files != null ? files.stream().mapToLong(MultipartFile::getSize).sum() : 0;
            log.info("执行上传作业业务：assignmentId={}, groupId={}, studentId={}, 文件数={}, 总大小={} bytes",
                    assignmentId, groupId, studentId, files != null ? files.size() : 0, totalFileSize);
            long startTime = System.currentTimeMillis();
            StudentSubmission submission = submissionService.uploadSubmission(assignmentId, groupId, studentId, submissionContent, files);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("上传作业成功：assignmentId={}, groupId={}, studentId={}, submissionId={}, 耗时={} ms",
                    assignmentId, groupId, studentId, submission.getSubmissionId(), costTime);
            return Result.success(submission);
        } catch (IllegalArgumentException e) {
            log.warn("上传作业参数错误：assignmentId={}, groupId={}, studentId={}, 错误：{}", assignmentId, groupId, studentId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("上传作业业务异常：assignmentId={}, groupId={}, studentId={}, 错误：{}", assignmentId, groupId, studentId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败：assignmentId={}, groupId={}, studentId={}, 错误：{}", assignmentId, groupId, studentId, e.getMessage(), e);
            return Result.error(500, "文件上传失败：" + e.getMessage());
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
        log.debug("收到更新提交请求：URI=/api/submission/{}, 参数：submissionId={}, 有文件={}",
                submissionId, files != null && !files.isEmpty());
        try {
            log.info("执行更新提交业务：submissionId={}", submissionId);
            long startTime = System.currentTimeMillis();
            String result = submissionService.updateSubmission(submissionId, submissionContent, files);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("更新提交成功：submissionId={}, 耗时={} ms", submissionId, costTime);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("更新提交业务异常：submissionId={}, 错误：{}", submissionId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新提交失败：submissionId={}, 错误：{}", submissionId, e.getMessage(), e);
            return Result.error(500, "更新提交失败：" + e.getMessage());
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
        String groupComment = body.get("groupComment");
        log.debug("收到更新小组评语请求：URI=/api/submission/{}/comment, 参数：submissionId={}, groupComment 长度={}",
                submissionId, groupComment != null ? groupComment.length() : 0);
        try {
            log.info("执行更新小组评语业务：submissionId={}", submissionId);
            String result = submissionService.updateGroupComment(submissionId, groupComment);
            log.debug("更新小组评语成功：submissionId={}", submissionId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("更新小组评语参数错误：submissionId={}, 错误：{}", submissionId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新小组评语业务异常：submissionId={}, 错误：{}", submissionId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新小组评语失败：submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "更新小组评语失败：" + e.getMessage());
        }
    }

    /**
     * 学生/教师：获取提交详情
     */
    @GetMapping("/detail/{submissionId}")
    public Result<StudentSubmission> getSubmissionDetail(@PathVariable Long submissionId) {
        log.debug("收到获取提交详情请求：URI=/api/submission/detail/{}, 参数：submissionId={}", submissionId, submissionId);
        try {
            if (submissionId == null) {
                log.warn("获取提交详情参数错误：submissionId 为空");
                return Result.error(400, "submissionId 不能为空");
            }

            log.info("执行获取提交详情业务：submissionId={}", submissionId);
            StudentSubmission submission = submissionService.selectById(submissionId);
            if (submission == null) {
                log.warn("未找到提交记录：submissionId={}", submissionId);
                return Result.error(404, "未找到该提交记录");
            }

            log.info("获取提交详情成功：submissionId={}", submissionId);
            return Result.success(submission);
        } catch (Exception e) {
            log.error("获取提交详情失败：submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "获取提交详情失败：" + e.getMessage());
        }
    }

    /**
     * 教师：获取小组整体评语
     */
    @GetMapping("/{submissionId}/comment")
    public Result<Map<String, String>> getGroupComment(@PathVariable Long submissionId) {
        log.debug("收到获取小组评语请求：URI=/api/submission/{}/comment, 参数：submissionId={}", submissionId, submissionId);
        try {
            log.info("执行获取小组评语业务：submissionId={}", submissionId);
            Map<String, String> comment = submissionService.getGroupComment(submissionId);
            log.debug("获取小组评语成功：submissionId={}", submissionId);
            return Result.success(comment);
        } catch (RuntimeException e) {
            log.warn("获取小组评语业务异常：submissionId={}, 错误：{}", submissionId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("获取小组评语失败：submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "获取小组评语失败：" + e.getMessage());
        }
    }

    /**
     * 学生：查询自己小组的所有提交记录
     */
    @GetMapping("/my-group")
    public Result<List<StudentSubmission>> getMyGroupSubmissions(@RequestParam Long groupId) {
        log.debug("收到查询小组提交记录请求：URI=/api/submission/my-group, 参数：groupId={}", groupId);
        try {
            if (groupId == null) {
                log.warn("查询小组提交记录参数错误：groupId 为空");
                return Result.error(400, "groupId 不能为空");
            }

            log.info("执行查询小组提交记录业务：groupId={}", groupId);
            List<StudentSubmission> submissions = submissionService.listByGroupId(groupId);

            log.info("查询小组提交记录成功：groupId={}, 找到{}条记录", groupId, submissions != null ? submissions.size() : 0);
            return Result.success(submissions != null ? submissions : List.of());
        } catch (Exception e) {
            log.error("查询小组提交记录失败：groupId={}, error={}", groupId, e.getMessage(), e);
            return Result.error(500, "查询小组提交记录失败：" + e.getMessage());
        }
    }

    /**
     * 教师：查看某个作业的所有个人提交记录（按提交人）
     */
    @GetMapping("/personal-submission/by-assignment")
    public Result<List<PersonalSubmissionDTO>> getPersonalSubmissionsByAssignment(@RequestParam Long assignmentId) {
        log.debug("收到查询作业个人提交记录请求：URI=/api/submission/personal-submission/by-assignment, 参数：assignmentId={}", assignmentId);
        try {
            if (assignmentId == null) {
                log.warn("查询作业个人提交记录参数错误：assignmentId 为空");
                return Result.error(400, "assignmentId 不能为空");
            }

            log.info("执行查询作业个人提交记录业务：assignmentId={}", assignmentId);
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

            log.info("查询作业的个人提交记录成功：assignmentId={}, 找到{}条记录", assignmentId, dtoList.size());
            return Result.success(dtoList);
        } catch (Exception e) {
            log.error("查询作业个人提交记录失败：assignmentId={}, error={}", assignmentId, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }
}
