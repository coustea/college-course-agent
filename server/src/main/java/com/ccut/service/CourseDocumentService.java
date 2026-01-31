package com.ccut.service;

import com.ccut.entity.CourseDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程文档服务接口
 */
public interface CourseDocumentService {

    /**
     * 上传文档
     * @param courseId 课程ID
     * @param docIndex 文档序号（可选）
     * @param docTitle 文档标题（可选）
     * @param file 文档文件
     * @return 上传后的课程文档
     */
    CourseDocument insertDocument(Long courseId, Integer docIndex, String docTitle, MultipartFile file);

    /**
     * 更新文档
     * @param documentId 文档ID
     * @param docIndex 文档序号（可选）
     * @param docTitle 文档标题（可选）
     * @param file 文档文件（可选）
     * @return 更新结果消息
     */
    String updateDocument(Long documentId, Integer docIndex, String docTitle, MultipartFile file);

    /**
     * 删除文档
     * @param documentId 文档ID
     * @return 删除结果消息
     */
    String deleteDocument(Long documentId);

    /**
     * 按课程列出文档
     * @param courseId 课程ID
     * @return 文档列表
     */
    List<CourseDocument> listByCourseId(Long courseId);

}
