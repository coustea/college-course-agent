package com.ccut.controller;

import com.ccut.entity.CourseDocument;
import com.ccut.entity.Result;
import com.ccut.mapper.CourseDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 改造为：上传文档文件，courseId 必填、文件必填，其它（docIndex、docTitle）选填
    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CourseDocument> insert(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "docIndex", required = false) Integer docIndex,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestPart("file") MultipartFile file
    ){
        try {
            if (courseId == null) return Result.error(400, "courseId 必填");
            if (file == null || file.isEmpty()) return Result.error(400, "文件必填");

            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get("uploads").toAbsolutePath();
            Files.createDirectories(baseDir);
            Path uploadDir = baseDir.resolve(dateDir);
            Files.createDirectories(uploadDir);

            String original = file.getOriginalFilename();
            String ext = null;
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            }
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (ext != null && !ext.isEmpty()) {
                filename = filename + "." + ext;
            }
            Path target = uploadDir.resolve(filename);
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());

            String url = "/uploads/" + dateDir + "/" + filename;

            CourseDocument doc = new CourseDocument();
            doc.setCourseId(courseId);
            // 自动分配 docIndex：未传则取该课程最大值+1；若无记录则置 1
            if (docIndex == null) {
                Integer max = courseDocumentMapper.findMaxIndexByCourseId(courseId);
                int next = (max == null || max <= 0) ? 1 : max + 1;
                doc.setDocIndex(next);
            } else {
                doc.setDocIndex(docIndex);
            }
            doc.setDocTitle(docTitle);
            doc.setDocUrl(url);
            doc.setUploadDate(new Date());
            int n = courseDocumentMapper.insert(doc);
            if (n>0) return Result.success(doc);
            return Result.error(500, "添加失败");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    // 改造：PUT multipart，按 documentId 更新，文件可选且不修改 courseId
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> update(
            @RequestParam("documentId") Long documentId,
            @RequestParam(value = "docIndex", required = false) Integer docIndex,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestPart(value = "file", required = false) MultipartFile file
    ){
        try {
            if (documentId == null) return Result.error(400, "documentId 必填");

            CourseDocument doc = new CourseDocument();
            doc.setDocumentId(documentId);
            doc.setDocIndex(docIndex);
            doc.setDocTitle(docTitle);

            if (file != null && !file.isEmpty()) {
                String dateDir = LocalDate.now().toString();
                Path baseDir = Paths.get("uploads").toAbsolutePath();
                Files.createDirectories(baseDir);
                Path uploadDir = baseDir.resolve(dateDir);
                Files.createDirectories(uploadDir);

                String original = file.getOriginalFilename();
                String ext = null;
                if (original != null && original.contains(".")) {
                    ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                }
                String filename = UUID.randomUUID().toString().replace("-", "");
                if (ext != null && !ext.isEmpty()) {
                    filename = filename + "." + ext;
                }
                Path target = uploadDir.resolve(filename);
                Files.createDirectories(target.getParent());
                file.transferTo(target.toFile());

                String url = "/uploads/" + dateDir + "/" + filename;
                doc.setDocUrl(url);
                doc.setUploadDate(new Date());
            }

            int n = courseDocumentMapper.updateById(doc);
            if (n>0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("documentId") Long documentId){
        try {
            int n = courseDocumentMapper.deleteById(documentId);
            if (n>0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<java.util.List<CourseDocument>> list(@RequestParam("courseId") Long courseId){
        try {
            return Result.success(courseDocumentMapper.findByCourseId(courseId));
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}


