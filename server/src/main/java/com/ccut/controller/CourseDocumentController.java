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
        log.debug("收到上传文档请求：URI=/api/course/document/insert, 参数：courseId={}, docIndex={}, docTitle={}, 文件名={}, 文件大小={} bytes",
                courseId, docIndex, docTitle, file.getOriginalFilename(), file.getSize());
        try {
            log.info("执行上传文档业务：courseId={}, fileName={}, fileSize={} bytes", courseId, file.getOriginalFilename(), file.getSize());
            long startTime = System.currentTimeMillis();
            CourseDocument document = courseDocumentService.insertDocument(courseId, docIndex, docTitle, file);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("上传文档成功：courseId={}, documentId={}, fileName={}, 耗时={} ms", courseId, document.getDocumentId(), file.getOriginalFilename(), costTime);
            return Result.success(document);
        } catch (IllegalArgumentException e) {
            log.warn("上传文档参数错误：courseId={}, 错误：{}", courseId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("上传文档业务异常：courseId={}, 错误：{}", courseId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("上传文档异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
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
        log.debug("收到更新文档请求：URI=/api/course/document/update, 参数：documentId={}, docIndex={}, docTitle={}, 有文件={}",
                documentId, docIndex, docTitle, file != null && !file.isEmpty());
        try {
            log.info("执行更新文档业务：documentId={}", documentId);
            long startTime = System.currentTimeMillis();
            String result = courseDocumentService.updateDocument(documentId, docIndex, docTitle, file);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("更新文档成功：documentId={}, 耗时={} ms", documentId, costTime);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("更新文档参数错误：documentId={}, 错误：{}", documentId, e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("更新文档业务异常：documentId={}, 错误：{}", documentId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新文档异常：documentId={}, 错误：{}", documentId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("documentId") Long documentId) {
        log.debug("收到删除文档请求：URI=/api/course/document/delete, 参数：documentId={}", documentId);
        try {
            log.info("执行删除文档业务：documentId={}", documentId);
            String result = courseDocumentService.deleteDocument(documentId);
            log.debug("删除文档成功：documentId={}", documentId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("删除文档业务异常：documentId={}, 错误：{}", documentId, e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("删除文档异常：documentId={}, 错误：{}", documentId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 按课程列出文档
     */
    @GetMapping("/list")
    public Result<List<CourseDocument>> list(@RequestParam("courseId") Long courseId) {
        log.debug("收到查询文档列表请求：URI=/api/course/document/list, 参数：courseId={}", courseId);
        try {
            log.info("执行查询文档列表业务：courseId={}", courseId);
            List<CourseDocument> documents = courseDocumentService.listByCourseId(courseId);
            log.debug("查询文档列表成功：courseId={}, 文档数={}", courseId, documents.size());
            return Result.success(documents);
        } catch (Exception e) {
            log.error("查询文档列表异常：courseId={}, 错误：{}", courseId, e.getMessage(), e);
            return Result.error(500, e.getMessage());
        }
    }
}
