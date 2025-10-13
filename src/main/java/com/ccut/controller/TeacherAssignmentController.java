package com.ccut.controller;

import com.ccut.entity.FileInfo;
import com.ccut.entity.Result;
import com.ccut.entity.Teacher;
import com.ccut.entity.TeacherAssignment;
import com.ccut.service.Impl.TeacherAssignmentServiceImpl;
import com.ccut.service.Impl.TeacherServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private TeacherServiceImpl teacherService;

    @Autowired
    private TeacherAssignmentServiceImpl teacherAssignmentService;

    // 允许配置基础上传路径
    @Value("${file.upload-dir}")
    private String uploadBaseDir;

    @PostMapping("/byClassName")
    public Result<List<TeacherAssignment>> getAssignmentsByClassName(@RequestParam String className) {
        List<TeacherAssignment> teacherAssignments = teacherAssignmentService.selectByClassName(className);
        if (teacherAssignments == null) {
            log.error("未找到，className is {}", className);
            return Result.error(404, "未找到");
        }
        return Result.success(teacherAssignments);
    }

    @PostMapping
    public Result<TeacherAssignment> insert(
            @RequestParam Long teacherId,
            @RequestParam String assignmentName,
            @RequestParam(required = false) String requirements,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") String dueDate
    ) throws Exception {

        Teacher teacher = teacherService.selectById(teacherId);
        if (teacher == null) {
            log.error("未找到，teacherId is {}", teacherId);
            return Result.error(404, "未找到");
        }

        List<FileInfo> fileInfos = new ArrayList<>();

        if (files != null && files.length > 0) {
            //  使用配置文件路径作为根目录
            Path basePath = Paths.get(uploadBaseDir, "homework", "teacher");
            String dateDir = LocalDate.now().toString();
            Path uploadDir = basePath.resolve(dateDir);
            Files.createDirectories(uploadDir);

            log.info("文件保存路径: {}", uploadDir);

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

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

                // 数据库存相对路径（供前端访问）
                String relativePath = "/uploads/homework/teacher/" + dateDir + "/" + saveFileName;
                fileInfos.add(new FileInfo(originalFilename, relativePath, ext, file.getSize()));
            }
        }

        String attachmentJson = objectMapper.writeValueAsString(fileInfos);
        TeacherAssignment teacherAssignment = new TeacherAssignment();
        teacherAssignment.setTeacherId(teacherId);
        teacherAssignment.setAssignmentName(assignmentName);
        teacherAssignment.setRequirements(requirements);
        teacherAssignment.setAttachmentFiles(attachmentJson);
        teacherAssignment.setTeacherName(teacher.getName());

        if (dueDate != null && !dueDate.isEmpty()) {
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

            Map<String, Object> assignmentMap = new HashMap<>();
            assignmentMap.put("assignmentId", assignment.getAssignmentId());
            assignmentMap.put("teacherId", assignment.getTeacherId());
            assignmentMap.put("assignmentName", assignment.getAssignmentName());
            assignmentMap.put("description", assignment.getDescription());
            assignmentMap.put("requirements", assignment.getRequirements());
            assignmentMap.put("dueDate", assignment.getDueDate());
            assignmentMap.put("attachments", attachmentList);

            responseList.add(assignmentMap);
        }
        return Result.success(responseList);
    }

    @GetMapping("/by-course/{courseId}")
    public Result<List<Map<String, Object>>> selectByCourseId(@PathVariable Long courseId) {
        try {
            List<TeacherAssignment> list = teacherAssignmentService.selectByCourseId(courseId);
            List<Map<String, Object>> resp = new ArrayList<>();
            for (TeacherAssignment assignment : list) {
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
                Map<String, Object> map = new HashMap<>();
                map.put("assignmentId", assignment.getAssignmentId());
                map.put("teacherId", assignment.getTeacherId());
                map.put("assignmentName", assignment.getAssignmentName());
                map.put("description", assignment.getDescription());
                map.put("requirements", assignment.getRequirements());
                map.put("dueDate", assignment.getDueDate());
                map.put("attachments", attachmentList);
                resp.add(map);
            }
            return Result.success(resp);
        } catch (Exception e) {
            log.error("按课程查询作品失败, courseId: {}", courseId, e);
            return Result.error(500, "查询失败");
        }
    }

    @PutMapping("/{assignmentId}")
    public Result<TeacherAssignment> update(
            @PathVariable Long assignmentId,
            @RequestBody TeacherAssignment teacherAssignment
    ) {
        try {
            if (assignmentId == null || teacherAssignment == null) {
                log.error("参数错误,teacherAssignment: {}", teacherAssignment);
                return Result.error(400, "参数错误");
            }
            teacherAssignment.setAssignmentId(assignmentId);
            teacherAssignmentService.update(teacherAssignment);
            return Result.success(teacherAssignment);
        } catch (Exception e) {
            log.error("更新失败,assignmentId: {},teacherAssignment: {}", assignmentId, teacherAssignment, e);
            return Result.error(500, "更新失败");
        }
    }

    @DeleteMapping("/{assignmentId}")
    public Result<TeacherAssignment> delete(@PathVariable Long assignmentId) {
        try {
            if (assignmentId == null) {
                log.error("参数错误,assignmentId: {}", assignmentId);
                return Result.error(400, "参数错误");
            }

            teacherAssignmentService.delete(assignmentId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除失败,assignmentId: {},e: {}", assignmentId, e);
            return Result.error(500, "删除失败");
        }
    }

    @GetMapping
    public Result<List<Map<String, Object>>> selectAll() {
        try {
            List<TeacherAssignment> list = teacherAssignmentService.selectAll();
            List<Map<String, Object>> resp = new ArrayList<>();
            for (TeacherAssignment assignment : list) {
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
                Map<String, Object> map = new HashMap<>();
                map.put("assignmentId", assignment.getAssignmentId());
                map.put("teacherId", assignment.getTeacherId());
                map.put("assignmentName", assignment.getAssignmentName());
                map.put("description", assignment.getDescription());
                map.put("requirements", assignment.getRequirements());
                map.put("dueDate", assignment.getDueDate());
                map.put("attachments", attachmentList);
                resp.add(map);
            }
            return Result.success(resp);
        } catch (Exception e) {
            log.error("查询全部作品失败", e);
            return Result.error(500, "查询失败");
        }
    }
}
