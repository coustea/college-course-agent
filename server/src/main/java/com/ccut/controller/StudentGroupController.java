package com.ccut.controller;

import com.ccut.entity.GroupMember;
import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.entity.StudentGroup;
import com.ccut.exception.BusinessException;
import com.ccut.service.Impl.GroupMemberServiceImpl;
import com.ccut.service.Impl.StudentGroupServiceImpl;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.Impl.TeacherServiceImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 学生小组控制器
 */
@RestController
@RequestMapping("/api/student-group")
@Slf4j
public class StudentGroupController {

    @Autowired
    private StudentGroupServiceImpl studentGroupService;

    @Autowired
    private GroupMemberServiceImpl groupMemberService;

    @Autowired
    private StudentServiceImpl studentService;

    @Autowired
    private TeacherServiceImpl teacherService;

    @GetMapping
    public Result<List<StudentGroup>> selectAll() {
        List<StudentGroup> studentGroups = studentGroupService.selectAll();
        return Result.success(studentGroups);
    }

    @PostMapping("/getByGroupId")
    public Result<StudentGroup> getGroup(@RequestParam Long groupId) {
        if(groupId == null){
            throw new IllegalArgumentException("参数错误");
        }
        StudentGroup studentGroup = studentGroupService.selectByGroupId(groupId);
        if(studentGroup == null){
            throw new BusinessException(404, "未找到");
        }
        return Result.success(studentGroup);
    }

    @PostMapping
    @Transactional
    public Result<StudentGroup> create(@RequestBody CreateGroupRequest req) {
        String groupName = req.getGroupName();
        Long groupLeaderId = req.getGroupLeaderId();
        String groupDescription = req.getGroupDescription();
        List<Long> memberIds = req.getMemberIds();
        // === 参数校验 ===
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("参数错误：groupName 不能为空");
        }
        if (groupLeaderId == null) {
            throw new IllegalArgumentException("参数错误：groupLeaderId 不能为空");
        }
        if (groupDescription == null || groupDescription.isEmpty()) {
            throw new IllegalArgumentException("参数错误：groupDescription 不能为空");
        }
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("参数错误：memberIds 不能为空");
        }

        // === 1. 验证组长是否存在 ===
        Student leader = studentService.selectById(groupLeaderId);
        if (leader == null) {
            throw new IllegalArgumentException("参数错误：groupLeaderId 不存在，ID=" + groupLeaderId);
        }

        // === 2. 组长是否已在其他小组 ===
        GroupMember leaderExisting = groupMemberService.selectById(groupLeaderId);
        if (leaderExisting != null) {
            throw new IllegalArgumentException("该组长已加入其他小组");
        }

        // === 3. 创建小组 ===
        StudentGroup studentGroup = new StudentGroup();
        studentGroup.setGroupLeaderId(groupLeaderId);
        studentGroup.setClassName(leader.getClassName());
        studentGroup.setGroupName(groupName);
        studentGroup.setCreatedAt(new Date());
        studentGroup.setGroupDescription(groupDescription);
        studentGroup.setStatus(StudentGroup.GroupStatus.active);
        studentGroup.setApprovalStatus(StudentGroup.GroupApprovalStatus.pending);

        int insertResult = studentGroupService.insert(studentGroup);
        if (insertResult <= 0) {
            throw new RuntimeException("插入分组失败");
        }

        Long groupId = studentGroup.getGroupId();

        // === 4. 插入组长到 group_members 表 ===
        GroupMember leaderMember = new GroupMember(
                groupId,
                groupLeaderId,
                leader.getName(),
                leader.getStudentNumber(),
                leader.getClassName(),
                GroupMember.GroupMemberRole.leader,
                GroupMember.Status.approval
        );
        int leaderInsert = groupMemberService.insertMember(leaderMember);
        if (leaderInsert <= 0) {
            throw new RuntimeException("插入组长成员失败");
        }
        // 更新组长状态为已加入
        leader.setGroupStatus("approval");
        studentService.updateById(leader);

        // === 5. 插入其他成员 ===
        for (Long memberId : memberIds) {
            Student member = studentService.selectById(memberId);
            if (member == null) {
                log.warn("成员不存在，memberId={}", memberId);
                continue;
            }

            // 检查该成员是否已加入其他小组
            GroupMember existing = groupMemberService.selectById(memberId);
            if (existing != null) {
                throw new IllegalArgumentException("成员 " + member.getName() + " 已在其他小组中");
            }

            GroupMember memberEntry = new GroupMember(
                    groupId,
                    memberId,
                    member.getName(),
                    member.getStudentNumber(),
                    member.getClassName(),
                    GroupMember.GroupMemberRole.member,
                    GroupMember.Status.approval
            );
            int memberInsert = groupMemberService.insertMember(memberEntry);
            if (memberInsert <= 0) {
                throw new RuntimeException("插入成员失败：" + member.getName());
            }
            // 更新成员状态为已加入
            member.setGroupStatus("approval");
            studentService.updateById(member);
        }

        studentGroup = studentGroupService.selectByGroupId(groupId);
        return Result.success(studentGroup);
    }

    @PutMapping("/{groupId}")
    public Result<StudentGroup> update(@PathVariable("groupId") Long groupId, @RequestBody StudentGroup studentGroup) {
        if (studentGroup == null) {
            throw new IllegalArgumentException("参数错误");
        }
        studentGroup.setGroupId(groupId);
        StudentGroup group = studentGroupService.selectByGroupId(groupId);
        if (group == null) {
            throw new BusinessException(404, "用户分组不存在!");
        }
        studentGroup.setGroupId(groupId);
        int result = studentGroupService.update(studentGroup);
        if (result <= 0) {
            throw new RuntimeException("更新失败");
        }
        return Result.success(studentGroupService.selectByGroupId(groupId));
    }

    /**
     * 一次性更改小组的多项信息
     */
    @PutMapping("/{groupId}/full-update")
    @Transactional
    public Result<StudentGroup> fullUpdate(
            @PathVariable("groupId") Long groupId,
            @RequestBody FullUpdateRequest body
    ) {
        StudentGroup group = studentGroupService.selectByGroupId(groupId);
        if (group == null) {
            throw new BusinessException(404, "用户分组不存在!");
        }

        // 1) 更新组的基础信息
        StudentGroup patch = new StudentGroup();
        patch.setGroupId(groupId);
        if (body.getGroupName() != null && !body.getGroupName().isEmpty()) patch.setGroupName(body.getGroupName());
        if (body.getGroupDescription() != null && !body.getGroupDescription().isEmpty()) patch.setGroupDescription(body.getGroupDescription());
        if (body.getGroupLeaderId() != null) patch.setGroupLeaderId(body.getGroupLeaderId());
        if (body.getApprovalStatus() != null) patch.setApprovalStatus(body.getApprovalStatus());
        if (body.getStatus() != null) patch.setStatus(body.getStatus());
        if (patch.getGroupName() != null || patch.getGroupDescription() != null || patch.getApprovalStatus() != null || patch.getStatus() != null) {
            studentGroupService.update(patch);
        }

        // 2) 成员处理（增删）
        if (body.getRemoveMemberIds() != null) {
            for (Long sid : body.getRemoveMemberIds()) {
                if (sid == null) continue;
                groupMemberService.deleteMember(sid, groupId);
            }
        }
        if (body.getAddMemberIds() != null && !body.getAddMemberIds().isEmpty()) {
            for (Long sid : body.getAddMemberIds()) {
                if (sid == null) continue;
                // 若该学生已在任一小组，禁止重复加入（与创建逻辑一致）
                GroupMember existing = groupMemberService.selectById(sid);
                if (existing != null) {
                    throw new IllegalArgumentException("成员已在其他小组中：" + sid);
                }
                // 填充成员姓名/班级
                Student member = studentService.selectById(sid);
                if (member == null) {
                    throw new IllegalArgumentException("成员不存在：" + sid);
                }
                GroupMember item = new GroupMember(
                        groupId,
                        sid,
                        member.getName(),
                        member.getStudentNumber(),
                        member.getClassName(),
                        GroupMember.GroupMemberRole.member,
                        GroupMember.Status.approval
                );
                groupMemberService.insertMember(item);
                // 同步更新学生的分组状态为 approval
                try {
                    member.setGroupStatus("approval");
                    studentService.updateById(member);
                } catch (Exception ignore) {}
            }
        }

        return Result.success(studentGroupService.selectByGroupId(groupId));
    }

    @Data
    public static class FullUpdateRequest {
        private String groupName;
        private String groupDescription;
        private Long groupLeaderId;
        private StudentGroup.GroupStatus status;
        private StudentGroup.GroupApprovalStatus approvalStatus;
        private java.util.List<Long> addMemberIds;
        private java.util.List<Long> removeMemberIds;
    }

    @Data
    public static class CreateGroupRequest {
        private String groupName;
        private Long groupLeaderId;
        private String groupDescription;
        private List<Long> memberIds;
    }

    @PostMapping("/approvalStatus")
    public Result<List<StudentGroup>> selectByApprovalStatus(@RequestParam(value = "approvalStatus",defaultValue = "") StudentGroup.GroupApprovalStatus approvalStatus) {
        List<StudentGroup> studentGroups;
        if (approvalStatus == null) {
            studentGroups = studentGroupService.selectAll();
        } else {
            studentGroups = studentGroupService.selectByApprovalStatus(approvalStatus);
        }
        return Result.success(studentGroups);
    }

    @DeleteMapping("/{groupId}")
    public Result<String> delete(@PathVariable("groupId") Long groupId)  {
        int result = studentGroupService.deleteByGroupId(groupId);
        if (result <= 0) {
            throw new RuntimeException("未找到指定的学生分组");
        }
        return Result.success("删除成功");
    }
}
