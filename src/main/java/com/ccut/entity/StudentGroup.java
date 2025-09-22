package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGroup {
    private Long id;
    private Long courseId;
    private Long studentId;
    private String groupName; // 可选：分组名
    private Date createdAt;
}


