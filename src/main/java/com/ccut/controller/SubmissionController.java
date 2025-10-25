package com.ccut.controller;

import com.ccut.entity.FileInfo;
import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.entity.StudentSubmission;
import com.ccut.mapper.StudentSubmissionMapper;
import com.ccut.service.Impl.StudentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/submission")
public class SubmissionController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private StudentSubmissionMapper submissionMapper;

    @Autowired
    private StudentServiceImpl studentService;

    /**
     *  教师：查看所有学生提交的作业
     */
    @GetMapping
    public Result<List<StudentSubmission>> selectAll() {
        try {
            List<StudentSubmission> submissions = submissionMapper.selectAll();
            for (StudentSubmission submission : submissions) {
                System.out.println(
                        submission.getSubmissionFiles()
                );
            }
            return Result.success(submissions);
        } catch (Exception e) {
            log.error("查询所有作业提交失败: {}", e.getMessage(), e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     *  教师：根据 assignmentId 查看提交
     */
    @GetMapping("/{assignmentId}")
    public Result<List<StudentSubmission>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        try {
            List<StudentSubmission> submissions = submissionMapper.selectByAssignmentId(assignmentId);
            return Result.success(submissions);
        } catch (Exception e) {
            log.error("查询作业提交失败 assignmentId={}: {}", assignmentId, e.getMessage(), e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     *  学生：上传作业
     */
    @PostMapping("/upload")
    public Result<StudentSubmission> upload(
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("groupId") Long groupId,
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "content", required = false) String submissionContent,
            @RequestParam("files") List<MultipartFile> files
    ) {
        if (assignmentId == null || studentId == null) {
            log.warn("assignmentId 与 studentId 不能为空");
            return Result.error(400, "assignmentId 与 studentId 不能为空");
        }

        try {
            // ==== 构建上传路径 ====
            String baseUploadPath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get(baseUploadPath, "submission", dateDir);
            Files.createDirectories(baseDir);

            List<FileInfo> fileInfos = new ArrayList<>();

            // ====  保存文件 ====
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file.isEmpty()) continue;

                    String originalFilename = file.getOriginalFilename();
                    String ext = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
                    }

                    // 生成唯一文件名
                    String filename = UUID.randomUUID().toString().replace("-", "");
                    if (!ext.isEmpty()) filename += "." + ext;

                    Path target = baseDir.resolve(filename);
                    file.transferTo(target.toFile());

                    // 用于前端直接访问的路径
                    String relativePath = "/uploads/submission/" + dateDir + "/" + filename;
                    fileInfos.add(new FileInfo(originalFilename, relativePath, ext, file.getSize()));
                }
            }

            // ====  转JSON ====
            String submissionFilesJson = fileInfos.isEmpty() ? null :
                    new ObjectMapper().writeValueAsString(fileInfos);

            // ====  校验学生 ====
            Student student = studentService.selectById(studentId);
            if (student == null) {
                log.error("学生不存在: {}", studentId);
                return Result.error(404, "学生不存在");
            }

            // ====  校验重复提交 ====
            StudentSubmission existing = submissionMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
            if (existing != null) {
                log.warn("小组已提交，不能重复提交: assignmentId={}, groupId={}", assignmentId, groupId);
                return Result.error(400, "小组已提交，不能重复提交");
            }

            // ====  插入提交记录 ====
            StudentSubmission studentSubmission = new StudentSubmission();
            studentSubmission.setAssignmentId(assignmentId);
            studentSubmission.setGroupId(groupId);
            studentSubmission.setSubmittedBy(studentId);
            studentSubmission.setClassName(student.getClassName());
            studentSubmission.setSubmissionContent(submissionContent);
            studentSubmission.setSubmissionFiles(submissionFilesJson);

            int inserted = submissionMapper.insert(studentSubmission);
            if (inserted > 0 && studentSubmission.getSubmissionId() != null) {
                StudentSubmission saved = submissionMapper.selectById(studentSubmission.getSubmissionId());
                return Result.success(saved);
            }
            return Result.error(500, "提交失败");

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
            // 若有文件，保存并生成新的 JSON
            String submissionFilesJson = null;
            if (files != null && !files.isEmpty()) {
                String baseUploadPath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
                String dateDir = LocalDate.now().toString();
                Path baseDir = Paths.get(baseUploadPath, "submission", dateDir);
                Files.createDirectories(baseDir);

                List<FileInfo> fileInfos = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;
                    String originalFilename = file.getOriginalFilename();
                    String ext = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
                    }
                    String filename = UUID.randomUUID().toString().replace("-", "");
                    if (!ext.isEmpty()) filename += "." + ext;
                    Path target = baseDir.resolve(filename);
                    file.transferTo(target.toFile());
                    String relativePath = "/uploads/submission/" + dateDir + "/" + filename;
                    fileInfos.add(new FileInfo(originalFilename, relativePath, ext, file.getSize()));
                }
                submissionFilesJson = fileInfos.isEmpty() ? null : new ObjectMapper().writeValueAsString(fileInfos);
            }

            StudentSubmission patch = new StudentSubmission();
            patch.setSubmissionId(submissionId);
            if (submissionContent != null) patch.setSubmissionContent(submissionContent);
            if (submissionFilesJson != null) patch.setSubmissionFiles(submissionFilesJson);

            int n = submissionMapper.updateById(patch);
            return n > 0 ? Result.success("更新成功") : Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            log.error("更新提交失败: {}", e.getMessage(), e);
            return Result.error(500, "更新提交失败: " + e.getMessage());
        }
    }

    /**
     * 教师：添加或更新小组整体评语
     * PUT /api/submission/{submissionId}/comment
     * @param submissionId 提交ID
     * @param groupComment 小组评语（JSON body: {"groupComment": "评语内容"}）
     * @return 操作结果
     */
    @PutMapping("/{submissionId}/comment")
    public Result<String> updateGroupComment(
            @PathVariable Long submissionId,
            @RequestBody java.util.Map<String, String> body
    ) {
        try {
            String groupComment = body.get("groupComment");
            
            if (groupComment == null) {
                return Result.error(400, "缺少 groupComment 参数");
            }
            
            // 检查评语长度（最多500字）
            if (groupComment.length() > 500) {
                return Result.error(400, "评语长度不能超过500字");
            }
            
            // 检查提交是否存在
            StudentSubmission existing = submissionMapper.selectById(submissionId);
            if (existing == null) {
                return Result.error(404, "未找到该提交记录");
            }
            
            // 更新评语
            StudentSubmission update = new StudentSubmission();
            update.setSubmissionId(submissionId);
            update.setGroupComment(groupComment);
            
            int n = submissionMapper.updateById(update);
            if (n > 0) {
                log.info("小组评语更新成功: submissionId={}, commentLength={}", submissionId, groupComment.length());
                return Result.success("小组评语更新成功");
            } else {
                return Result.error(500, "更新失败");
            }
        } catch (Exception e) {
            log.error("更新小组评语失败: submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "更新小组评语失败: " + e.getMessage());
        }
    }

    /**
     * 学生/教师：获取提交详情
     * GET /api/submission/detail/{submissionId}
     * @param submissionId 提交ID
     * @return 提交详情
     */
    @GetMapping("/detail/{submissionId}")
    public Result<StudentSubmission> getSubmissionDetail(@PathVariable Long submissionId) {
        try {
            if (submissionId == null) {
                return Result.error(400, "submissionId 不能为空");
            }
            
            StudentSubmission submission = submissionMapper.selectById(submissionId);
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
     * GET /api/submission/{submissionId}/comment
     * @param submissionId 提交ID
     * @return 评语内容
     */
    @GetMapping("/{submissionId}/comment")
    public Result<java.util.Map<String, String>> getGroupComment(@PathVariable Long submissionId) {
        try {
            StudentSubmission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                return Result.error(404, "未找到该提交记录");
            }
            
            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("groupComment", submission.getGroupComment() != null ? submission.getGroupComment() : "");
            result.put("submissionId", String.valueOf(submissionId));
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取小组评语失败: submissionId={}, error={}", submissionId, e.getMessage(), e);
            return Result.error(500, "获取小组评语失败: " + e.getMessage());
        }
    }

    /**
     * 学生：查询自己小组的所有提交记录
     * GET /api/submission/my-group?groupId={groupId}
     * @param groupId 小组ID
     * @return 该小组的所有提交记录列表
     */
    @GetMapping("/my-group")
    public Result<List<StudentSubmission>> getMyGroupSubmissions(@RequestParam Long groupId) {
        try {
            if (groupId == null) {
                return Result.error(400, "groupId 不能为空");
            }
            
            // 查询该小组的所有提交记录
            List<StudentSubmission> submissions = submissionMapper.listByGroupId(groupId);
            
            log.info("查询小组提交记录: groupId={}, 找到{}条记录", groupId, submissions != null ? submissions.size() : 0);
            return Result.success(submissions != null ? submissions : new ArrayList<>());
        } catch (Exception e) {
            log.error("查询小组提交记录失败: groupId={}, error={}", groupId, e.getMessage(), e);
            return Result.error(500, "查询小组提交记录失败: " + e.getMessage());
        }
    }
}
