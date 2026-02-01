package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 错题本实体类
 * 自动收集学生答错的题目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrongQuestion {
    private Long id;
    private Long studentId;        // 学生ID
    private Long questionId;       // 题目ID（关联 ai_exam_question）
    private Long examId;           // 考试ID（关联 ai_exam）
    private Long courseId;         // 课程ID（冗余字段，方便查询）
    private String wrongAnswer;    // 学生的错误答案
    private String correctAnswer;  // 正确答案（冗余字段，方便查看）
    private Integer wrongCount;    // 错误次数（每次答错累加）
    private Integer correctCount;  // 连续答对次数（复习功能，答对累加，答错重置为0）
    private Boolean isMastered;    // 是否已掌握
    private Date firstWrongTime;   // 首次答错时间
    private Date lastWrongTime;    // 最后答错时间
    private Date masteredTime;     // 掌握时间
    private Date createTime;       // 创建时间
    private Date updateTime;       // 更新时间

    // 关联的题目详细信息（用于查询时返回）
    private AiExamQuestion question;
}
