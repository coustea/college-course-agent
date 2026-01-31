package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProgress {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long documentId;
    private Integer timeSpent;
    private Double maxScrollPct;
    private Boolean completed;
    private Date updatedAt;
    private Double percentage; // 计算字段：可用 maxScrollPct 或其他口径
}


