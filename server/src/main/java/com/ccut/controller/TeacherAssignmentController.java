package com.ccut.controller;

import com.ccut.dto.FileInfo;
import com.ccut.dto.Result;
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
        log.debug("收到按班级查询作业请求：URI=/api/teacherAssignments/byClassName, 参数：className={}", className);
        try {
            log.info("执行按班级查询作业业务：className={}", className);
            List<TeacherAssignment> teacherAssignments = teacherAssignmentService.selectByClassName(className);
            if (teacherAssignments == null) {
                log.error("未找到作业：className={}", className);
                return Result.error(404, "未找到");
            }
            log.debug("按班级查询作业成功：className={}, 结果数={}", className, teacherAssignments.size());
            return Result.success(teacherAssignments);
        } catch (Exception e) {
            log.error("按班级查询作业异常：className={}, 错误：{}", className, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<TeacherAssignment> insert(
            @RequestParam Long teacherId,
            @RequestParam String assignmentName,
            @RequestParam(required = false) String requirements,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") String dueDate
    ) throws Exception {
        log.debug("收到插入作业请求：URI=/api/teacherAssignments, 参数：teacherId={}, assignmentName={}, 文件数={}",
                teacherId, assignmentName, files != null ? files.length : 0);
        try {
            log.info("执行插入作业业务：teacherId={}, assignmentName={}", teacherId, assignmentName);

            Teacher teacher = teacherService.selectById(teacherId);
            if (teacher == null) {
                log.error("未找到教师：teacherId={}", teacherId);
                return Result.error(404, "未找到");
            }

            List<FileInfo> fileInfos = new ArrayList<>();

            if (files != null && files.length > 0) {
                //  使用配置文件路径作为根目录
                Path basePath = Paths.get(uploadBaseDir, "homework", "teacher");
                String dateDir = LocalDate.now().toString();
                Path uploadDir = basePath.resolve(dateDir);
                Files.createDirectories(uploadDir);

                log.info("文件保存路径：{}", uploadDir);

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
                    log.debug("文件上传成功：originalFilename={}, size={} bytes", originalFilename, file.getSize());
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
            log.debug("插入作业成功：teacherId={}, assignmentName={}, assignmentId={}", teacherId, assignmentName, teacherAssignment.getAssignmentId());
            return Result.success(teacherAssignment);
        } catch (Exception e) {
            log.error("插入作业异常：teacherId={}, assignmentName={}, 错误：{}", teacherId, assignmentName, e.getMessage(), e);
            return Result.error(500, "添加失败：" + e.getMessage());
        }
    }

    @GetMapping("/{teacherId}")
    public Result<List<Map<String, Object>>> selectByTeacherId(@PathVariable Long teacherId) {
        log.debug("收到查询教师作业请求：URI=/api/teacherAssignments/{}, 参数：teacherId={}", teacherId, teacherId);
        try {
            log.info("执行查询教师作业业务：teacherId={}", teacherId);
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
                        log.error("解析附件 JSON 失败：{}", attachmentJson, e);
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
            log.debug("查询教师作业成功：teacherId={}, 结果数={}", teacherId, responseList.size());
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("查询教师作业异常：teacherId={}, 错误：{}", teacherId, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/by-course/{courseId}")
    public Result<List<Map<String, Object>>> selectByCourseId(@PathVariable Long courseId) {
        log.debug("收到查询课程作业请求：URI=/api/teacherAssignments/by-course/{}, 参数：courseId={}", courseId, courseId);
        try {
            log.info("执行查询课程作业业务：courseId={}", courseId);
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
                        log.error("解析附件 JSON 失败：{}", attachmentJson, e);
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
            log.debug("查询课程作业成功：courseId={}, 结果数={}", courseId, resp.size());
            return Result.success(resp);
        } catch (Exception e) {
            log.error("按课程查询作品失败，courseId: {}", courseId, e);
            return Result.error(500, "查询失败");
        }
    }

    @PutMapping("/{assignmentId}")
    public Result<TeacherAssignment> update(
            @PathVariable Long assignmentId,
            @RequestBody TeacherAssignment teacherAssignment
    ) {
        log.debug("收到更新作业请求：URI=/api/teacherAssignments/{}, 参数：assignmentId={}, teacherAssignment={}", assignmentId, teacherAssignment);
        try {
            if (assignmentId == null || teacherAssignment == null) {
                log.error("参数错误，assignmentId={}, teacherAssignment={}", assignmentId, teacherAssignment);
                return Result.error(400, "参数错误");
            }
            teacherAssignment.setAssignmentId(assignmentId);
            log.info("执行更新作业业务：assignmentId={}", assignmentId);
            teacherAssignmentService.update(teacherAssignment);
            log.debug("更新作业成功：assignmentId={}", assignmentId);
            return Result.success(teacherAssignment);
        } catch (Exception e) {
            log.error("更新失败，assignmentId: {},teacherAssignment: {}", assignmentId, teacherAssignment, e);
            return Result.error(500, "更新失败");
        }
    }

    @DeleteMapping("/{assignmentId}")
    public Result<TeacherAssignment> delete(@PathVariable Long assignmentId) {
        log.debug("收到删除作业请求：URI=/api/teacherAssignments/{}, 参数：assignmentId={}", assignmentId, assignmentId);
        try {
            if (assignmentId == null) {
                log.error("参数错误，assignmentId: {}", assignmentId);
                return Result.error(400, "参数错误");
            }

            log.info("执行删除作业业务：assignmentId={}", assignmentId);
            teacherAssignmentService.delete(assignmentId);
            log.debug("删除作业成功：assignmentId={}", assignmentId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除失败，assignmentId: {},e: {}", assignmentId, e);
            return Result.error(500, "删除失败");
        }
    }

    @GetMapping
    public Result<List<Map<String, Object>>> selectAll() {
        log.debug("收到查询所有作业请求：URI=/api/teacherAssignments");
        try {
            log.info("执行查询所有作业业务");
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
                        log.error("解析附件 JSON 失败：{}", attachmentJson, e);
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
            log.debug("查询所有作业成功：结果数={}", resp.size());
            return Result.success(resp);
        } catch (Exception e) {
            log.error("查询全部作品失败", e);
            return Result.error(500, "查询失败");
        }
    }
}
