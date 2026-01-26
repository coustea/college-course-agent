package com.ccut.controller;

import com.ccut.entity.CourseDocument;
import com.ccut.dto.Result;
import com.ccut.mapper.CourseDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/course/document")
public class CourseDocumentController {

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    //  从配置文件读取文件保存路径
    @Value("${file.upload-dir}")
    private String uploadDir;

    //  上传文档接口
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CourseDocument> insert(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "docIndex", required = false) Integer docIndex,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            if (courseId == null) return Result.error(400, "courseId 必填");
            if (file == null || file.isEmpty()) return Result.error(400, "文件必填");

            // 用配置路径而不是相对路径
            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(baseDir);

            Path uploadDateDir = baseDir.resolve(dateDir);
            Files.createDirectories(uploadDateDir);

            String original = file.getOriginalFilename();
            String ext = null;
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            }
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (ext != null && !ext.isEmpty()) filename = filename + "." + ext;

            Path target = uploadDateDir.resolve(filename);
            file.transferTo(target.toFile());

            //  URL 与 WebMvcConfig 一致
            String url = "/uploads/" + dateDir + "/" + filename;

            CourseDocument doc = new CourseDocument();
            doc.setCourseId(courseId);
            // 自动计算 docIndex
            if (docIndex == null) {
                Integer max = courseDocumentMapper.findMaxIndexByCourseId(courseId);
                doc.setDocIndex((max == null || max <= 0) ? 1 : max + 1);
            } else {
                doc.setDocIndex(docIndex);
            }
            doc.setDocTitle(docTitle);
            doc.setDocUrl(url);
            doc.setUploadDate(new Date());

            int n = courseDocumentMapper.insert(doc);
            if (n > 0) return Result.success(doc);
            return Result.error(500, "添加失败");

        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 更新接口
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam("documentId") Long documentId,
            @RequestParam(value = "docIndex", required = false) Integer docIndex,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            if (documentId == null) return Result.error(400, "documentId 必填");

            CourseDocument doc = new CourseDocument();
            doc.setDocumentId(documentId);
            doc.setDocIndex(docIndex);
            doc.setDocTitle(docTitle);

            if (file != null && !file.isEmpty()) {
                String dateDir = LocalDate.now().toString();
                Path baseDir = Paths.get(uploadDir).toAbsolutePath();
                Files.createDirectories(baseDir);

                Path uploadDateDir = baseDir.resolve(dateDir);
                Files.createDirectories(uploadDateDir);

                String original = file.getOriginalFilename();
                String ext = null;
                if (original != null && original.contains(".")) {
                    ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                }
                String filename = UUID.randomUUID().toString().replace("-", "");
                if (ext != null && !ext.isEmpty()) filename = filename + "." + ext;

                Path target = uploadDateDir.resolve(filename);
                file.transferTo(target.toFile());

                String url = "/uploads/" + dateDir + "/" + filename;
                doc.setDocUrl(url);
                doc.setUploadDate(new Date());
            }

            int n = courseDocumentMapper.updateById(doc);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");

        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    //  删除
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("documentId") Long documentId) {
        try {
            int n = courseDocumentMapper.deleteById(documentId);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }


    @GetMapping("/list")
    public Result<java.util.List<CourseDocument>> list(@RequestParam("courseId") Long courseId) {
        try {
            return Result.success(courseDocumentMapper.findByCourseId(courseId));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
