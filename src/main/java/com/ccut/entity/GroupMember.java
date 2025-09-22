package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMember {

    enum GroupMemberRole {
        LEADER, MEMBER
    }
    private Long id;            // 成员记录ID
    private Long groupId;       // 小组ID
    private Long courseId;      // 课程ID
    private Long studentId;     // 学生ID
    private Date joinTime;      // 加入时间
    private GroupMemberRole role;        // 角色 leader/member
    private String joinStatus;
}