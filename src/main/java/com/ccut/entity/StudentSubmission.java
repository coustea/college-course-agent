package com.ccut.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentSubmission {

    public enum Status{
        // 已提交
        submitted,
        // 已评分
        graded,
        // 已返回
        returned,
        // 已重提交
        resubmitted
    }

    // 提交ID
    private Long submissionId;
    // 作业ID
    private Long assignmentId;
    // 小组ID
    private Long groupId;
    // 学生ID
    private Long studentId;
    // 提交内容
    private String submissionContent;
    // 提交文件
    private String submissionFiles;
    // 提交时间
    private LocalDateTime submittedAt;
    // 迟交
    private Boolean lateSubmission;
    // 分数
    private Double score;
    // 反馈
    private String feedback;
    // 评分时间
    private LocalDateTime gradedAt;
    // 评分人ID
    private Long gradedBy;
    // 状态
    private String status;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
}
