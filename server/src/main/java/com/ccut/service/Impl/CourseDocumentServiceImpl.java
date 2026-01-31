package com.ccut.service.Impl;

import com.ccut.entity.CourseDocument;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.CourseDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 课程文档服务实现类
 */
@Service
public class CourseDocumentServiceImpl implements CourseDocumentService {

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public CourseDocument insertDocument(Long courseId, Integer docIndex, String docTitle, MultipartFile file) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId 必填");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件必填");
        }

        String dateDir = LocalDate.now().toString();
        Path baseDir = Paths.get(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(baseDir);
        } catch (Exception e) {
            throw new RuntimeException("创建目录失败: " + e.getMessage());
        }

        Path uploadDateDir = baseDir.resolve(dateDir);
        try {
            Files.createDirectories(uploadDateDir);
        } catch (Exception e) {
            throw new RuntimeException("创建目录失败: " + e.getMessage());
        }

        String original = file.getOriginalFilename();
        String ext = null;
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (ext != null && !ext.isEmpty()) {
            filename = filename + "." + ext;
        }

        Path target = uploadDateDir.resolve(filename);
        try {
            file.transferTo(target.toFile());
        } catch (Exception e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        // URL 与 WebMvcConfig 一致
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
        if (n > 0) {
            return doc;
        }
        throw new RuntimeException("添加失败");
    }

    @Override
    @Transactional
    public String updateDocument(Long documentId, Integer docIndex, String docTitle, MultipartFile file) {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId 必填");
        }

        CourseDocument doc = new CourseDocument();
        doc.setDocumentId(documentId);
        doc.setDocIndex(docIndex);
        doc.setDocTitle(docTitle);

        if (file != null && !file.isEmpty()) {
            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get(uploadDir).toAbsolutePath();
            try {
                Files.createDirectories(baseDir);
            } catch (Exception e) {
                throw new RuntimeException("创建目录失败: " + e.getMessage());
            }

            Path uploadDateDir = baseDir.resolve(dateDir);
            try {
                Files.createDirectories(uploadDateDir);
            } catch (Exception e) {
                throw new RuntimeException("创建目录失败: " + e.getMessage());
            }

            String original = file.getOriginalFilename();
            String ext = null;
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            }
            String filename = UUID.randomUUID().toString().replace("-", "");
            if (ext != null && !ext.isEmpty()) {
                filename = filename + "." + ext;
            }

            Path target = uploadDateDir.resolve(filename);
            try {
                file.transferTo(target.toFile());
            } catch (Exception e) {
                throw new RuntimeException("文件保存失败: " + e.getMessage());
            }

            String url = "/uploads/" + dateDir + "/" + filename;
            doc.setDocUrl(url);
            doc.setUploadDate(new Date());
        }

        int n = courseDocumentMapper.updateById(doc);
        if (n > 0) {
            return "更新成功";
        }
        throw new RuntimeException("未找到或未变更");
    }

    @Override
    @Transactional
    public String deleteDocument(Long documentId) {
        int n = courseDocumentMapper.deleteById(documentId);
        if (n > 0) {
            return "删除成功";
        }
        throw new RuntimeException("未找到");
    }

    @Override
    public List<CourseDocument> listByCourseId(Long courseId) {
        return courseDocumentMapper.findByCourseId(courseId);
    }

}
