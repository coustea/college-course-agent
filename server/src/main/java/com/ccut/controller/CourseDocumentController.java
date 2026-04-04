package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.entity.CourseDocument;
import com.ccut.service.CourseDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程文档控制器
 */
@RestController
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
        CourseDocument document = courseDocumentService.insertDocument(courseId, docIndex, docTitle, file);
        return Result.success(document);
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
        String result = courseDocumentService.updateDocument(documentId, docIndex, docTitle, file);
        return Result.success(result);
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("documentId") Long documentId) {
        String result = courseDocumentService.deleteDocument(documentId);
        return Result.success(result);
    }

    /**
     * 按课程列出文档
     */
    @GetMapping("/list")
    public Result<List<CourseDocument>> list(@RequestParam("courseId") Long courseId) {
        List<CourseDocument> documents = courseDocumentService.listByCourseId(courseId);
        return Result.success(documents);
    }
}
