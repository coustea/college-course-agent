package com.ccut.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {


    @Getter
    public enum Status {
        pending(1), active(2), completed(3), cancelled(4);

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
            throw new IllegalArgumentException("无效码: " + code);
        }
    }

    private Integer id;
    private String title;
    private String term;
    private Status status;

    private List<Teacher> teachers;
}
