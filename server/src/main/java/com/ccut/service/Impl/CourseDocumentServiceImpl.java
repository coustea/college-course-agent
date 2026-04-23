package com.ccut.service.Impl;

import com.ccut.entity.CourseDocument;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.CourseDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CourseDocumentServiceImpl.class);

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public CourseDocument insertDocument(Long courseId, Integer docIndex, String docTitle, MultipartFile file) {
        log.debug("执行方法：insertDocument, 参数：courseId={}, docIndex={}, docTitle={}, file={}", 
                courseId, docIndex, docTitle, file != null ? file.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (courseId == null) {
                log.error("参数验证失败：courseId 必填");
                throw new IllegalArgumentException("courseId 必填");
            }
            if (file == null || file.isEmpty()) {
                log.error("参数验证失败：文件必填");
                throw new IllegalArgumentException("文件必填");
            }

            String dateDir = LocalDate.now().toString();
            Path baseDir = Paths.get(uploadDir).toAbsolutePath();
            try {
                Files.createDirectories(baseDir);
            } catch (Exception e) {
                log.error("创建目录失败：path={}", baseDir);
                throw new RuntimeException("创建目录失败：" + e.getMessage());
            }

            Path uploadDateDir = baseDir.resolve(dateDir);
            try {
                Files.createDirectories(uploadDateDir);
            } catch (Exception e) {
                log.error("创建目录失败：path={}", uploadDateDir);
                throw new RuntimeException("创建目录失败：" + e.getMessage());
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
                file.transferTo(target);
                log.info("文档文件保存成功：originalFilename={}, savedFilename={}", original, filename);
            } catch (Exception e) {
                log.error("文件保存失败：filename={}", filename);
                throw new RuntimeException("文件保存失败：" + e.getMessage());
            }

            // URL 与 WebMvcConfig 一致
            String url = "/uploads/" + dateDir + "/" + filename;

            CourseDocument doc = new CourseDocument();
            doc.setCourseId(courseId);
            // 自动计算 docIndex
            if (docIndex == null) {
                Integer max = courseDocumentMapper.findMaxIndexByCourseId(courseId);
                doc.setDocIndex((max == null || max <= 0) ? 1 : max + 1);
                log.info("自动计算文档序号：docIndex={}", doc.getDocIndex());
            } else {
                doc.setDocIndex(docIndex);
            }
            doc.setDocTitle(docTitle);
            doc.setDocUrl(url);
            doc.setUploadDate(new Date());

            int n = courseDocumentMapper.insert(doc);
            long duration = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("文档插入成功：documentId={}, courseId={}, docTitle={}, 耗时={}ms", 
                        doc.getDocumentId(), courseId, docTitle, duration);
                log.debug("方法返回：result={}", doc);
                return doc;
            }
            log.error("文档插入失败：courseId={}, docTitle={}", courseId, docTitle);
            throw new RuntimeException("添加失败");
        } catch (Exception e) {
            log.error("文档插入异常：courseId={}, docTitle={}, error={}", courseId, docTitle, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String updateDocument(Long documentId, Integer docIndex, String docTitle, MultipartFile file) {
        log.debug("执行方法：updateDocument, 参数：documentId={}, docIndex={}, docTitle={}, file={}", 
                documentId, docIndex, docTitle, file != null ? file.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (documentId == null) {
                log.error("参数验证失败：documentId 必填");
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
                    log.error("创建目录失败：path={}", baseDir);
                    throw new RuntimeException("创建目录失败：" + e.getMessage());
                }

                Path uploadDateDir = baseDir.resolve(dateDir);
                try {
                    Files.createDirectories(uploadDateDir);
                } catch (Exception e) {
                    log.error("创建目录失败：path={}", uploadDateDir);
                    throw new RuntimeException("创建目录失败：" + e.getMessage());
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
                    file.transferTo(target);
                    log.info("新文档文件保存成功：originalFilename={}, savedFilename={}", original, filename);
                } catch (Exception e) {
                    log.error("文件保存失败：filename={}", filename);
                    throw new RuntimeException("文件保存失败：" + e.getMessage());
                }

                String url = "/uploads/" + dateDir + "/" + filename;
                doc.setDocUrl(url);
                doc.setUploadDate(new Date());
                log.info("文档文件已更新：url={}", url);
            }

            int n = courseDocumentMapper.updateById(doc);
            long duration = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("文档更新成功：documentId={}, 耗时={}ms", documentId, duration);
                log.debug("方法返回：result=更新成功");
                return "更新成功";
            }
            log.warn("文档未找到或未变更：documentId={}, 耗时={}ms", documentId, duration);
            throw new RuntimeException("未找到或未变更");
        } catch (Exception e) {
            log.error("文档更新异常：documentId={}, error={}", documentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String deleteDocument(Long documentId) {
        log.debug("执行方法：deleteDocument, 参数：documentId={}", documentId);
        long startTime = System.currentTimeMillis();
        try {
            int n = courseDocumentMapper.deleteById(documentId);
            long duration = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("文档删除成功：documentId={}, 耗时={}ms", documentId, duration);
                log.debug("方法返回：result=删除成功");
                return "删除成功";
            }
            log.warn("文档未找到：documentId={}, 耗时={}ms", documentId, duration);
            throw new RuntimeException("未找到");
        } catch (Exception e) {
            log.error("文档删除失败：documentId={}, error={}", documentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<CourseDocument> listByCourseId(Long courseId) {
        log.debug("执行方法：listByCourseId, 参数：courseId={}", courseId);
        long startTime = System.currentTimeMillis();
        try {
            List<CourseDocument> result = courseDocumentMapper.findByCourseId(courseId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询课程文档列表成功：courseId={}, count={}, 耗时={}ms", courseId, result != null ? result.size() : 0, duration);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询课程文档列表失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

}
