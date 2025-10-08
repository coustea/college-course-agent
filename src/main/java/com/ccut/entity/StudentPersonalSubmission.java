package com.ccut.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentPersonalSubmission {

    public enum Status{
        submitted,
        graded,
        returned,
        resubmitted
    }

    private Long submissionId;
    private Long assignmentId;
    private Long studentId;
    private String submissionContent;
    private String submissionFiles; // 可存 JSON 字符串或逗号分隔
    private LocalDateTime submittedAt;
    private Boolean lateSubmission;
    private Integer score;
    private String feedback;
    private LocalDateTime gradedAt;
    private Long gradedBy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


