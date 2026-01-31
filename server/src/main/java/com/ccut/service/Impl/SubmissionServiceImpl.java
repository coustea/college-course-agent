package com.ccut.service.Impl;

import com.ccut.dto.FileInfo;
import com.ccut.entity.Student;
import com.ccut.entity.StudentSubmission;
import com.ccut.mapper.StudentSubmissionMapper;
import com.ccut.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 作业提交服务实现类
 */
@Service
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private StudentSubmissionMapper studentSubmissionMapper;

    @Autowired
    private StudentServiceImpl studentService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public int insert(StudentSubmission studentSubmission) {
        return studentSubmissionMapper.insert(studentSubmission);
    }

    @Override
    @Transactional
    public StudentSubmission uploadSubmission(Long assignmentId, Long groupId, Long studentId,
                                              String submissionContent, List<MultipartFile> files) {
        if (assignmentId == null || studentId == null) {
            throw new IllegalArgumentException("assignmentId 与 studentId 不能为空");
        }

        // ==== 构建上传路径 ====
        String baseUploadPath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String dateDir = LocalDate.now().toString();
        Path baseDir = Paths.get(baseUploadPath, "submission", dateDir);
        try {
            Files.createDirectories(baseDir);
        } catch (Exception e) {
            throw new RuntimeException("创建目录失败: " + e.getMessage());
        }

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
                if (!ext.isEmpty()) {
                    filename += "." + ext;
                }

                Path target = baseDir.resolve(filename);
                try {
                    file.transferTo(target.toFile());
                } catch (Exception e) {
                    throw new RuntimeException("文件保存失败: " + e.getMessage());
                }

                // 用于前端直接访问的路径
                String relativePath = "/uploads/submission/" + dateDir + "/" + filename;
                fileInfos.add(new FileInfo(originalFilename, relativePath, ext, file.getSize()));
            }
        }

        // ====  转JSON ====
        String submissionFilesJson = null;
        try {
            submissionFilesJson = fileInfos.isEmpty() ? null :
                    new ObjectMapper().writeValueAsString(fileInfos);
        } catch (Exception e) {
            throw new RuntimeException("文件信息序列化失败: " + e.getMessage());
        }

        // ====  校验学生 ====
        Student student = studentService.selectById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // ====  校验重复提交 ====
        StudentSubmission existing = studentSubmissionMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
        if (existing != null) {
            throw new RuntimeException("小组已提交，不能重复提交");
        }

        // ====  插入提交记录 ====
        StudentSubmission studentSubmission = new StudentSubmission();
        studentSubmission.setAssignmentId(assignmentId);
        studentSubmission.setGroupId(groupId);
        studentSubmission.setSubmittedBy(studentId);
        studentSubmission.setClassName(student.getClassName());
        studentSubmission.setSubmissionContent(submissionContent);
        studentSubmission.setSubmissionFiles(submissionFilesJson);

        int inserted = studentSubmissionMapper.insert(studentSubmission);
        if (inserted > 0 && studentSubmission.getSubmissionId() != null) {
            return studentSubmissionMapper.selectById(studentSubmission.getSubmissionId());
        }
        throw new RuntimeException("提交失败");
    }

    @Override
    @Transactional
    public String updateSubmission(Long submissionId, String submissionContent, List<MultipartFile> files) {
        // 若有文件，保存并生成新的 JSON
        String submissionFilesJson = null;
        if (files != null && !files.isEmpty()) {
            String baseUploadPath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get(baseUploadPath, "submission", dateDir);
            try {
                Files.createDirectories(baseDir);
            } catch (Exception e) {
                throw new RuntimeException("创建目录失败: " + e.getMessage());
            }

            List<FileInfo> fileInfos = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String originalFilename = file.getOriginalFilename();
                String ext = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
                }
                String filename = UUID.randomUUID().toString().replace("-", "");
                if (!ext.isEmpty()) {
                    filename += "." + ext;
                }
                Path target = baseDir.resolve(filename);
                try {
                    file.transferTo(target.toFile());
                } catch (Exception e) {
                    throw new RuntimeException("文件保存失败: " + e.getMessage());
                }
                String relativePath = "/uploads/submission/" + dateDir + "/" + filename;
                fileInfos.add(new FileInfo(originalFilename, relativePath, ext, file.getSize()));
            }
            try {
                submissionFilesJson = fileInfos.isEmpty() ? null : new ObjectMapper().writeValueAsString(fileInfos);
            } catch (Exception e) {
                throw new RuntimeException("文件信息序列化失败: " + e.getMessage());
            }
        }

        StudentSubmission patch = new StudentSubmission();
        patch.setSubmissionId(submissionId);
        if (submissionContent != null) {
            patch.setSubmissionContent(submissionContent);
        }
        if (submissionFilesJson != null) {
            patch.setSubmissionFiles(submissionFilesJson);
        }

        int n = studentSubmissionMapper.updateById(patch);
        if (n > 0) {
            return "更新成功";
        }
        throw new RuntimeException("未找到或未变更");
    }

    @Override
    @Transactional
    public String updateGroupComment(Long submissionId, String groupComment) {
        if (groupComment == null) {
            throw new IllegalArgumentException("缺少 groupComment 参数");
        }

        // 检查评语长度（最多500字）
        if (groupComment.length() > 500) {
            throw new IllegalArgumentException("评语长度不能超过500字");
        }

        // 检查提交是否存在
        StudentSubmission existing = studentSubmissionMapper.selectById(submissionId);
        if (existing == null) {
            throw new RuntimeException("未找到该提交记录");
        }

        // 更新评语
        StudentSubmission update = new StudentSubmission();
        update.setSubmissionId(submissionId);
        update.setGroupComment(groupComment);

        int n = studentSubmissionMapper.updateById(update);
        if (n > 0) {
            log.info("小组评语更新成功: submissionId={}, commentLength={}", submissionId, groupComment.length());
            return "小组评语更新成功";
        }
        throw new RuntimeException("更新失败");
    }

    @Override
    public StudentSubmission selectByGroupId(Long groupId) {
        return studentSubmissionMapper.selectByGroupId(groupId);
    }

    @Override
    public StudentSubmission selectByAssignmentIdAndGroupId(Long assignmentId, Long groupId) {
        return studentSubmissionMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
    }

    @Override
    public List<StudentSubmission> selectByAssignmentId(Long assignmentId) {
        return studentSubmissionMapper.selectByAssignmentId(assignmentId);
    }

    @Override
    public List<StudentSubmission> selectAll() {
        return studentSubmissionMapper.selectAll();
    }

    @Override
    public StudentSubmission selectById(Long submissionId) {
        return studentSubmissionMapper.selectById(submissionId);
    }

    @Override
    public List<StudentSubmission> listByGroupId(Long groupId) {
        return studentSubmissionMapper.listByGroupId(groupId);
    }

    @Override
    public Map<String, String> getGroupComment(Long submissionId) {
        StudentSubmission submission = studentSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("未找到该提交记录");
        }

        Map<String, String> result = new HashMap<>();
        result.put("groupComment", submission.getGroupComment() != null ? submission.getGroupComment() : "");
        result.put("submissionId", String.valueOf(submissionId));

        return result;
    }

}
