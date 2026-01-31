package com.ccut.service;

import com.ccut.entity.StudentSubmission;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 作业提交服务接口
 */
public interface SubmissionService {

    /**
     * 插入提交记录
     * @param studentSubmission 提交记录
     * @return 插入行数
     */
    int insert(StudentSubmission studentSubmission);

    /**
     * 上传作业（带文件上传）
     * @param assignmentId 作业ID
     * @param groupId 小组ID
     * @param studentId 学生ID
     * @param submissionContent 提交内容（可选）
     * @param files 文件列表
     * @return 提交记录
     */
    StudentSubmission uploadSubmission(Long assignmentId, Long groupId, Long studentId,
                                       String submissionContent, List<MultipartFile> files);

    /**
     * 更新提交记录
     * @param submissionId 提交ID
     * @param submissionContent 提交内容（可选）
     * @param files 文件列表（可选）
     * @return 更新结果消息
     */
    String updateSubmission(Long submissionId, String submissionContent, List<MultipartFile> files);

    /**
     * 更新小组评语
     * @param submissionId 提交ID
     * @param groupComment 小组评语
     * @return 更新结果消息
     */
    String updateGroupComment(Long submissionId, String groupComment);

    /**
     * 根据小组ID查询提交
     * @param groupId 小组ID
     * @return 提交记录
     */
    StudentSubmission selectByGroupId(Long groupId);

    /**
     * 根据作业ID和小组ID查询提交
     * @param assignmentId 作业ID
     * @param groupId 小组ID
     * @return 提交记录
     */
    StudentSubmission selectByAssignmentIdAndGroupId(Long assignmentId, Long groupId);

    /**
     * 根据作业ID查询提交列表
     * @param assignmentId 作业ID
     * @return 提交记录列表
     */
    List<StudentSubmission> selectByAssignmentId(Long assignmentId);

    /**
     * 查询所有提交记录
     * @return 提交记录列表
     */
    List<StudentSubmission> selectAll();

    /**
     * 根据ID查询提交详情
     * @param submissionId 提交ID
     * @return 提交记录
     */
    StudentSubmission selectById(Long submissionId);

    /**
     * 查询小组的所有提交记录
     * @param groupId 小组ID
     * @return 提交记录列表
     */
    List<StudentSubmission> listByGroupId(Long groupId);

    /**
     * 获取小组评语
     * @param submissionId 提交ID
     * @return 包含评语的映射
     */
    Map<String, String> getGroupComment(Long submissionId);

}
