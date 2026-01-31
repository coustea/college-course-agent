package com.ccut.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 个人提交记录 DTO（用于教师端查看个人提交）
 * 字段名适配前端需求
 */
@Data
public class PersonalSubmissionDTO {
    // 学生ID（前端期望 studentId）
    private Long studentId;
    // 提交时间
    private LocalDateTime submittedAt;
    // 提交状态
    private String status;
    // 分数（如果有）
    private Integer score;
    // 提交内容
    private String submissionContent;
    // 提交文件（JSON字符串）
    private String submissionFiles;
}
