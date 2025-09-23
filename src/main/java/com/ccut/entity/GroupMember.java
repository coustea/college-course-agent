package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMember {

    public enum GroupMemberRole {
        leader, member
    }
    public enum Status{
        // 待审核，审核通过，审核拒绝
        pending,
        approved,
        rejected
    }
    private Long id;            // 成员记录ID
    private Long groupId;       // 小组ID
    private Long courseId;      // 课程ID
    private Long studentId;     // 学生ID
    private String studentName; // 学生姓名
    private Date joinTime;      // 加入时间
    private GroupMemberRole role;        // 角色 leader/member
    private Status joinStatus;


    public GroupMember(Long groupId, Long courseId, Long studentId, String studentName, GroupMemberRole role, Status joinStatus) {
        this.groupId = groupId;
        this.courseId = courseId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.role = role;
        this.joinStatus = joinStatus;
    }
}