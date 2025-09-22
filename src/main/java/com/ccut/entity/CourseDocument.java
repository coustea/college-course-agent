package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {
    private Long documentId;
    private Long courseId;
    private Integer docIndex;   // 映射到列 document_index
    private String docTitle;    // 映射到列 document_title
    private String docUrl;      // 映射到列 document_url
    private Date uploadDate;
}


