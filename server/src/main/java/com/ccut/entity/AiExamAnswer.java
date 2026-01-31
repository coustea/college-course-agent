package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExamAnswer {
    private Long id;
    private Long attemptId;
    private Long questionId;
    private String studentAnswer; // 学生答案
    private Boolean correct;      // AI 判定是否正确
}



