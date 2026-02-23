package com.ccut.controller;

import com.ccut.entity.GroupMember;
import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
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
            log.error("参数错误，groupId is null");
            return Result.error(400, "参数错误");
        }
        StudentGroup studentGroup = studentGroupService.selectByGroupId(groupId);
        if(studentGroup == null){
            log.error("未找到，groupId is {}", groupId);
            return Result.error(404, "未找到");
        }
        return Result.success(studentGroup);
    }

    @PostMapping
    @Transactional
    public Result<StudentGroup> create(@RequestParam String groupName,
                                       @RequestParam Long groupLeaderId,
                                       @RequestParam String groupDescription,
                                       @RequestParam List<Long> memberIds) {
        try {
            log.info("=== 创建小组请求 ===");
            log.info("groupName: {}", groupName);
            log.info("groupLeaderId: {} (类型: {})", groupLeaderId, groupLeaderId != null ? groupLeaderId.getClass().getName() : "null");
            log.info("groupDescription: {}", groupDescription);
            log.info("memberIds: {}", memberIds);

            // === 参数校验 ===
            if (groupName == null || groupName.isEmpty()) {
                return Result.error(400, "参数错误：groupName 不能为空");
            }
            if (groupLeaderId == null) {
                log.error("groupLeaderId 为 null！");
                return Result.error(400, "参数错误：groupLeaderId 不能为空");
            }
            if (groupDescription == null || groupDescription.isEmpty()) {
                return Result.error(400, "参数错误：groupDescription 不能为空");
            }
            if (memberIds == null || memberIds.isEmpty()) {
                return Result.error(400, "参数错误：memberIds 不能为空");
            }

            // === 1. 验证组长是否存在 ===
            log.info("正在查询组长，groupLeaderId = {}", groupLeaderId);
            Student leader = studentService.selectById(groupLeaderId);
            log.info("查询结果: leader = {}", leader);
            if (leader == null) {
                log.error("未找到 groupLeaderId={} 的学生记录", groupLeaderId);
                // 尝试查询所有学生，看看数据库中有哪些数据
                List<Student> allStudents = studentService.selectAll();
                log.error("数据库中共有 {} 个学生", allStudents.size());
                if (!allStudents.isEmpty()) {
                    log.error("前5个学生的ID: {}",
                        allStudents.stream().limit(5).map(s -> s.getId()).toList());
                }
                return Result.error(400, "参数错误：groupLeaderId 不存在，ID=" + groupLeaderId);
            }

            // === 2. 组长是否已在其他小组 ===
            GroupMember leaderExisting = groupMemberService.selectById(groupLeaderId);
            if (leaderExisting != null) {
                return Result.error(400, "该组长已加入其他小组");
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
                log.error("插入分组失败: {}", studentGroup);
                throw new RuntimeException("插入分组失败");
            }

            Long groupId = studentGroup.getGroupId();
            log.info("小组创建成功，groupId={}", groupId);

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
            try {
                int leaderInsert = groupMemberService.insertMember(leaderMember);
                if (leaderInsert <= 0) {
                    log.error("插入组长到 group_members 表失败: groupId={}, leaderId={}", groupId, groupLeaderId);
                    throw new RuntimeException("插入组长成员失败");
                }
                log.info("组长插入成功: groupId={}, leaderId={}", groupId, groupLeaderId);
            } catch (Exception e) {
                log.error("插入组长时发生异常: groupId={}, leaderId={}", groupId, groupLeaderId, e);
                throw new RuntimeException("插入组长成员失败: " + e.getMessage(), e);
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
                    return Result.error(400, "成员 " + member.getName() + " 已在其他小组中");
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
                    throw new RuntimeException("插入成员失败: " + member.getName());
                }
                // 更新成员状态为已加入
                member.setGroupStatus("approval");
                studentService.updateById(member);
            }

            log.info("创建小组及成员成功, groupId={}", groupId);
            studentGroup = studentGroupService.selectByGroupId(groupId);
            return Result.success(studentGroup);

        } catch (Exception e) {
            log.error("创建分组异常", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    @PutMapping("/{groupId}")
    public Result<StudentGroup> update(@PathVariable("groupId") Long groupId, @RequestBody StudentGroup studentGroup) {
        try{
            if (studentGroup == null) {
                log.error("参数错误，studentGroup is null");
                return Result.error(400, "参数错误");
            }
            log.info("groupId is {}",groupId);
            studentGroup.setGroupId(groupId);
            StudentGroup group = studentGroupService.selectByGroupId(groupId);
            if (group == null) {
                log.error("用户分组不存在!");
                return Result.error(404, "用户分组不存在!");
            }
            studentGroup.setGroupId(groupId);
            int result = studentGroupService.update(studentGroup);
            if (result > 0) {
                return Result.success(studentGroupService.selectByGroupId(groupId));
            }
            log.error("更新失败，studentGroup is {}", studentGroup);
            return Result.error(500, "更新失败");
        } catch (Exception e) {
            log.error("更新学生分组信息时发生异常: ", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 一次性更改小组的多项信息（名称/描述/审批状态/成员增删替换）。
     * - groupName / groupDescription / approvalStatus / status 可选
     * - membersReplace: 若为 true，则先清空该组所有成员后，按 members 重新插入；否则仅执行增删
     * - addMemberIds: 需要新增的学生ID列表
     * - removeMemberIds: 需要移除的学生ID列表
     */
    @PutMapping("/{groupId}/full-update")
    @Transactional
    public Result<StudentGroup> fullUpdate(
            @PathVariable("groupId") Long groupId,
            @RequestBody FullUpdateRequest body
    ) {
        try {
            log.info("=== 完整更新小组请求 ===");
            log.info("groupId: {}", groupId);
            log.info("请求体: groupName={}, groupDescription={}, groupLeaderId={}, approvalStatus={}",
                    body.getGroupName(), body.getGroupDescription(), body.getGroupLeaderId(), body.getApprovalStatus());
            log.info("新增成员: {}", body.getAddMemberIds());
            log.info("移除成员: {}", body.getRemoveMemberIds());

            StudentGroup group = studentGroupService.selectByGroupId(groupId);
            if (group == null) {
                log.error("小组不存在，groupId={}", groupId);
                return Result.error(404, "用户分组不存在!");
            }

            log.info("当前小组信息: name={}, approvalStatus={}", group.getGroupName(), group.getApprovalStatus());

            // 1) 更新组的基础信息
            StudentGroup patch = new StudentGroup();
            patch.setGroupId(groupId);
            if (body.getGroupName() != null && !body.getGroupName().isEmpty()) patch.setGroupName(body.getGroupName());
            if (body.getGroupDescription() != null && !body.getGroupDescription().isEmpty()) patch.setGroupDescription(body.getGroupDescription());
            if (body.getGroupLeaderId() != null) patch.setGroupLeaderId(body.getGroupLeaderId());
            if (body.getApprovalStatus() != null) patch.setApprovalStatus(body.getApprovalStatus());
            if (body.getStatus() != null) patch.setStatus(body.getStatus());
            if (patch.getGroupName() != null || patch.getGroupDescription() != null || patch.getApprovalStatus() != null || patch.getStatus() != null) {
                int updateResult = studentGroupService.update(patch);
                log.info("小组基础信息更新结果: {}", updateResult);
            }

            // 2) 成员处理（增删）
            if (body.getRemoveMemberIds() != null) {
                for (Long sid : body.getRemoveMemberIds()) {
                    if (sid == null) continue;
                    groupMemberService.deleteMember(sid, groupId);
                    log.info("移除成员: studentId={}", sid);
                }
            }
            if (body.getAddMemberIds() != null && !body.getAddMemberIds().isEmpty()) {
                for (Long sid : body.getAddMemberIds()) {
                    if (sid == null) continue;
                    log.info("添加成员: studentId={}", sid);
                    // 若该学生已在任一小组，禁止重复加入（与创建逻辑一致）
                    GroupMember existing = groupMemberService.selectById(sid);
                    if (existing != null) {
                        log.warn("成员已在其他小组中: studentId={}", sid);
                        return Result.error(400, "成员已在其他小组中: " + sid);
                    }
                    // 填充成员姓名/班级
                    Student member = studentService.selectById(sid);
                    if (member == null) {
                        log.warn("成员不存在: studentId={}", sid);
                        return Result.error(400, "成员不存在: " + sid);
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
                    log.info("成员添加成功: {}", item);
                    // 同步更新学生的分组状态为 approval
                    try {
                        member.setGroupStatus("approval");
                        studentService.updateById(member);
                    } catch (Exception ignore) {}
                }
            }

            return Result.success(studentGroupService.selectByGroupId(groupId));
        } catch (Exception e) {
            log.error("批量更新学生分组信息异常", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
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

    @PostMapping("/approvalStatus")
    public Result<List<StudentGroup>> selectByApprovalStatus(@RequestParam(value = "approvalStatus",defaultValue = "") StudentGroup.GroupApprovalStatus approvalStatus) {
        try {
            if (approvalStatus == null) {
                return Result.success(studentGroupService.selectAll());
            }
            List<StudentGroup> studentGroups = studentGroupService.selectByApprovalStatus(approvalStatus);
            return Result.success(studentGroups);
        } catch (Exception e) {
            log.error("查询学生分组信息时发生异常: ", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    @DeleteMapping("/{groupId}")
    public Result<String> delete(@PathVariable("groupId") Long groupId)  {
        try {
            int result = studentGroupService.deleteByGroupId(groupId);
            if (result > 0) {
                return Result.success("删除成功");
            }
            return Result.error(404, "未找到指定的学生分组");
        } catch (Exception e) {
            log.error("删除学生分组信息时发生异常: ", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }
}