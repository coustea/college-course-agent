package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGroup {

    // === 小组状态枚举 ===
    public enum GroupStatus {
        active, disbanded
    }

    // === 审批状态枚举 ===
    public enum GroupApprovalStatus {
        pending,   // 待审核
        approved,  // 审核通过
        rejected   // 审核拒绝
    }

    private Long groupId;              // 分组ID（自增主键）
    private String groupName;          // 小组名称
    private Long groupLeaderId;        // 组长ID
    private Long teacherId;            // 审核教师ID
    private String groupDescription;   // 小组描述
    private Date createdAt;            // 创建时间
    private Date updatedAt;            // 更新时间
    private GroupStatus status;        // 小组状态（active / disbanded）
    private GroupApprovalStatus approvalStatus; // 审批状态

    // === 新增：前端创建时携带的成员列表 ===
    private List<GroupMember> groupMemberList;
}
