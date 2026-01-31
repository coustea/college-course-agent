package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.CourseDocument;
import com.ccut.service.CourseDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程文档控制器
 */
@RestController
@Slf4j
@RequestMapping("/api/course/document")
public class CourseDocumentController {

    @Autowired
    private CourseDocumentService courseDocumentService;

    /**
     * 上传文档接口
     */
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CourseDocument> insert(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "docIndex", required = false) Integer docIndex,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            return Result.success(courseDocumentService.insertDocument(courseId, docIndex, docTitle, file));
        } catch (IllegalArgumentException e) {
            log.warn("上传文档参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("上传文档业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("上传文档异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 更新接口
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam("documentId") Long documentId,
            @RequestParam(value = "docIndex", required = false) Integer docIndex,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            return Result.success(courseDocumentService.updateDocument(documentId, docIndex, docTitle, file));
        } catch (IllegalArgumentException e) {
            log.warn("更新文档参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新文档业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新文档异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("documentId") Long documentId) {
        try {
            return Result.success(courseDocumentService.deleteDocument(documentId));
        } catch (RuntimeException e) {
            log.warn("删除文档业务异常: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("删除文档异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按课程列出文档
     */
    @GetMapping("/list")
    public Result<List<CourseDocument>> list(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(courseDocumentService.listByCourseId(courseId));
        } catch (Exception e) {
            log.error("查询文档列表异常", e);
            return Result.error(500, e.getMessage());
        }
    }

}
