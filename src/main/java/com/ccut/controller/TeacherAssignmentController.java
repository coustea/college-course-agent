package com.ccut.controller;


import com.ccut.entity.FileInfo;
import com.ccut.entity.Result;
import com.ccut.entity.TeacherAssignment;
import com.ccut.service.Impl.TeacherAssignmentServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/teacherAssignments")
@Slf4j
public class TeacherAssignmentController {

    private final ObjectMapper objectMapper = new ObjectMapper();



    @Autowired
    private TeacherAssignmentServiceImpl teacherAssignmentService;

    @PostMapping
    public Result<TeacherAssignment> insert(
            @RequestParam Long teacherId,
            @RequestParam Long courseId,
            @RequestParam String assignmentName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String requirements,
            @RequestParam(required = false) Boolean allowLateSubmission,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") String dueDate
    ) throws Exception {
        List<FileInfo> fileInfos = new ArrayList<>();

        if (files != null && files.length > 0) {
            // 获取项目运行目录
            Path projectRoot = Paths.get("").toAbsolutePath();
            log.info("projectRoot: {}", projectRoot);

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                // 按日期生成目录：uploads/homework/teacher/yyyy-MM-dd
                String dateDir = LocalDate.now().toString();
                Path uploadDir = projectRoot.resolve(Paths.get("uploads", "homework", "teacher", dateDir));
                Files.createDirectories(uploadDir);

                // 安全文件名：UUID + 原扩展名
                String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                String ext = StringUtils.getFilenameExtension(originalFilename);
                String saveFileName = UUID.randomUUID().toString().replace("-", "");
                if (StringUtils.hasText(ext)) {
                    saveFileName += "." + ext.toLowerCase();
                }

                // 写入文件
                Path target = uploadDir.resolve(saveFileName);
                file.transferTo(target.toFile());

                // 数据库存相对路径
                String relativePath = "uploads/homework/teacher/" + dateDir + "/" + saveFileName;
                fileInfos.add(new FileInfo(originalFilename, relativePath, ext, file.getSize()));
            }
        }

        String attachmentJson = objectMapper.writeValueAsString(fileInfos);

        TeacherAssignment teacherAssignment = new TeacherAssignment();
        teacherAssignment.setTeacherId(teacherId);
        teacherAssignment.setCourseId(courseId);
        teacherAssignment.setAssignmentName(assignmentName);
        teacherAssignment.setDescription(description);
        teacherAssignment.setRequirements(requirements);
        teacherAssignment.setAllowLateSubmission(allowLateSubmission != null ? allowLateSubmission : false);
        teacherAssignment.setAttachmentFiles(attachmentJson);

        if (dueDate != null && !dueDate.isEmpty()) {
            // 解析到分钟，秒固定为 0
            LocalDateTime dateTime = LocalDateTime.parse(dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            teacherAssignment.setDueDate(dateTime.withSecond(0));
        }

        teacherAssignment.setCreatedAt(LocalDateTime.now().withSecond(0));
        teacherAssignment.setUpdatedAt(LocalDateTime.now().withSecond(0));

        teacherAssignmentService.insert(teacherAssignment);

        return Result.success(teacherAssignment);
    }

    @GetMapping("/{teacherId}")
    public Result<List<Map<String, Object>>> selectByTeacherId(@PathVariable Long teacherId) {
        List<TeacherAssignment> teacherAssignmentList = teacherAssignmentService.selectByTeacherId(teacherId);
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (TeacherAssignment assignment : teacherAssignmentList) {
            List<Map<String, Object>> attachmentList = new ArrayList<>();
            String attachmentJson = assignment.getAttachmentFiles();
            if (attachmentJson != null && !attachmentJson.isEmpty()) {
                try {
                    attachmentList = objectMapper.readValue(
                            attachmentJson,
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                } catch (Exception e) {
                    log.error("解析附件JSON失败: {}", attachmentJson, e);
                }
            }

            // 用 HashMap 代替 Map.of，避免 null 报错
            Map<String, Object> assignmentMap = new HashMap<>();
            assignmentMap.put("assignmentId", assignment.getAssignmentId());
            assignmentMap.put("teacherId", assignment.getTeacherId());
            assignmentMap.put("courseId", assignment.getCourseId());
            assignmentMap.put("assignmentName", assignment.getAssignmentName());
            assignmentMap.put("description", assignment.getDescription());
            assignmentMap.put("requirements", assignment.getRequirements());
            assignmentMap.put("dueDate", assignment.getDueDate());
            assignmentMap.put("allowLateSubmission", assignment.getAllowLateSubmission());
            assignmentMap.put("attachments", attachmentList);

            responseList.add(assignmentMap);
        }

        return Result.success(responseList);
    }

    @PutMapping("/{assignmentId}")
    public Result<TeacherAssignment> update(
            @PathVariable Long assignmentId,
            @RequestBody TeacherAssignment teacherAssignment
    ) {
        try {
            if (assignmentId == null || teacherAssignment == null){
                log.error("参数错误,teacherAssignment: {} ",teacherAssignment);
                return Result.error(400, "参数错误");
            }
            teacherAssignment.setAssignmentId(assignmentId);
            teacherAssignmentService.update(teacherAssignment);
            return Result.success(teacherAssignment);
        } catch (Exception e) {
            log.error("更新失败,assignmentId: {},teacherAssignment: {}",assignmentId,teacherAssignment,e);
            return Result.error(500, "更新失败");
        }
    }

    @DeleteMapping("/{assignmentId}")
    public Result<TeacherAssignment> delete(
            @PathVariable Long assignmentId
    ) {
        try {
            if (assignmentId == null){
                log.error("参数错误,assignmentId: {} ",assignmentId);
                return Result.error(400, "参数错误");
            }

            teacherAssignmentService.delete(assignmentId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除失败,assignmentId: {},e: {}",assignmentId,e);
            return Result.error(500, "删除失败");
        }
    }
}
