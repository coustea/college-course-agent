package com.ccut.dto;

import java.time.LocalDateTime;

public record StudentMemberScore(
    Long memberId,
    Long submissionId,
    Long studentId,
    String studentName,
    Integer score,
    String level,
    String feedback,
    LocalDateTime gradedAt
) {}