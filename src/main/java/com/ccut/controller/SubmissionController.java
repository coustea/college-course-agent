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
    public Result<String> upload(
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
            return inserted > 0 ? Result.success("提交成功") : Result.error(500, "提交失败");

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}
