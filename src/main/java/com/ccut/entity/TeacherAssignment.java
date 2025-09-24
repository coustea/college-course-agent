package com.ccut.entity;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherAssignment {

    // 作业ID
    private Long assignmentId;
    // 教师ID
    private Long teacherId;
    // 课程ID
    private Long courseId;
    // 作业名称
    private String assignmentName;
    // 作业描述
    private String description;
    // 作业要求
    private String requirements;
    // 截止日期
    private LocalDateTime dueDate;
    // 是否允许迟交
    private Boolean allowLateSubmission;
    // 附件文件
    private String attachmentFiles;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
}
