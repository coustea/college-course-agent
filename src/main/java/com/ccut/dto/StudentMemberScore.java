package com.ccut.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentMemberScore {

    private Long memberId;
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private Integer score;
    private String level ;
    private String feedback;
    private LocalDateTime gradedAt;
}