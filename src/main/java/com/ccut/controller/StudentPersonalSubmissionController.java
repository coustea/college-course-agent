package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.StudentPersonalSubmission;
import com.ccut.mapper.StudentPersonalSubmissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/personal-submission")
public class StudentPersonalSubmissionController {

    @Autowired
    private StudentPersonalSubmissionMapper mapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<StudentPersonalSubmission> upload(
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "content", required = false) String submissionContent,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestPart(value = "file", required = false) MultipartFile singleFile
    ){
        try {
            if (assignmentId == null || studentId == null) return Result.error(400, "assignmentId 与 studentId 不能为空");

            List<String> fileUrls = new ArrayList<>();
            List<MultipartFile> toSave = new ArrayList<>();
            if (files != null) for (MultipartFile f : files) if (f != null && !f.isEmpty()) toSave.add(f);
            if (singleFile != null && !singleFile.isEmpty()) toSave.add(singleFile);

            if (!toSave.isEmpty()){
                String dateDir = LocalDate.now().toString();
                Path baseDir = Paths.get("uploads").toAbsolutePath();
                Files.createDirectories(baseDir);
                Path uploadDir = baseDir.resolve(dateDir);
                Files.createDirectories(uploadDir);
                for (MultipartFile f : toSave){
                    String original = f.getOriginalFilename();
                    String ext = null;
                    if (original != null && original.contains(".")) ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                    String filename = java.util.UUID.randomUUID().toString().replace("-", "");
                    if (ext != null && !ext.isEmpty()) filename = filename + "." + ext;
                    Path target = uploadDir.resolve(filename);
                    Files.createDirectories(target.getParent());
                    f.transferTo(target.toFile());
                    String url = "/uploads/" + dateDir + "/" + filename;
                    fileUrls.add(url);
                }
            }

            StudentPersonalSubmission s = new StudentPersonalSubmission();
            s.setAssignmentId(assignmentId);
            s.setStudentId(studentId);
            s.setSubmissionContent(submissionContent);
            // JSON 列要求有效 JSON：序列化为字符串数组，例如 ["/uploads/a.mp4", "/uploads/b.pdf"]
            String filesJson = null;
            if (!fileUrls.isEmpty()) {
                try {
                    filesJson = new ObjectMapper().writeValueAsString(fileUrls);
                } catch (Exception ignore) {
                    // 兜底：手动拼接为 JSON 数组
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < fileUrls.size(); i++) {
                        if (i > 0) sb.append(',');
                        sb.append('"').append(fileUrls.get(i).replace("\"", "\\\"")).append('"');
                    }
                    sb.append(']');
                    filesJson = sb.toString();
                }
            }
            s.setSubmissionFiles(filesJson);
            s.setSubmittedAt(LocalDateTime.now());
            s.setLateSubmission(Boolean.FALSE);
            s.setStatus("submitted");
            s.setCreatedAt(LocalDateTime.now());

            int n = mapper.upsert(s);
            if (n > 0) return Result.success(s);
            return Result.error(500, "保存失败");
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 教师评分接口：按 assignmentId + studentId 评分并反馈
    @PostMapping("/grade")
    public Result<String> grade(
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("studentId") Long studentId,
            @RequestParam("score") Integer score,
            @RequestParam(value = "feedback", required = false) String feedback,
            @RequestParam("gradedBy") Long gradedBy
    ){
        try {
            if (assignmentId == null || studentId == null) return Result.error(400, "assignmentId 与 studentId 不能为空");
            if (score == null) return Result.error(400, "score 不能为空");
            int n = mapper.grade(assignmentId, studentId, score, feedback, gradedBy);
            if (n > 0) return Result.success("评分成功");
            return Result.error(404, "未找到该提交");
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}



