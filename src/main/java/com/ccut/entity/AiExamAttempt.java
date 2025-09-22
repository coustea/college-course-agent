package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExamAttempt {
    private Long id;
    private Long examId;
    private Long studentId;
    private Integer score;       // AI 评卷后的得分
    private String resultJson;   // 评卷详情（可选：各题判定、得分）
    private Date submittedAt;
}



