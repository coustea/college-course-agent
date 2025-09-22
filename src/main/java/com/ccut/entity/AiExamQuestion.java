package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExamQuestion {
    private Long id;
    private Long examId;
    private String type;      // CHOICE/JUDGE
    private String content;   // 题干
    private String options;   // JSON 数组字符串
    private String answer;    // 正确答案（A/B/C/D 或 true/false）
    private String analysis;  // 解析
}



