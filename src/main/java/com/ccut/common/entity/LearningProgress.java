package com.ccut.common.entity;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LearningProgress {
    @Getter
    public enum Status {
        NOT_STARTED(0),
        IN_PROGRESS(1),
        PAUSED(2),
        COMPLETED(3);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public static Status fromCode(int code) {
            for (Status status : Status.values()) {
                if (status.getCode() == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("无效状态码: " + code);
        }
    }

    private Integer id;
    private BigDecimal completionPercentage;
    private Status status;
    private Long studentId;
    private Long courseId;

    private Student student;
    private Course course;
}