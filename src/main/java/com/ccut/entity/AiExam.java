package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExam {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long studentId; // 可为空：为课程生成的共享试卷时不指定学生
    private String topic;   // 一般使用课程名
    private Integer questionCount;
    private Integer totalScore; // 最近一次提交的得分（可选）
    private String status;  // generated / submitted
    private Date createdAt;
}



