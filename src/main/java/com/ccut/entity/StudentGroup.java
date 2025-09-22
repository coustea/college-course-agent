package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGroup {

    public enum GroupStatus {
        active, disbanded
    }
    public enum GroupApprovalStatus {
        // 待审核
        pending,
        // 审核通过
        approved,
        // 审核被拒绝
        rejected
    }
    private Long groupId;            // 分组ID
    private Long courseId;           // 课程ID
    private String groupName;        // 小组名称
    private Long groupLeaderId;      // 组长ID
    private Long teacherId;          // 审核教师ID
    private String groupDescription; // 小组描述
    private Date createdAt;          // 创建时间
    private Date updatedAt;          // 更新时间
    private GroupStatus status;           // 小组状态：active / disbanded
    private GroupApprovalStatus approvalStatus;
}
