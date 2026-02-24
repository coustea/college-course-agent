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
        log.debug("收到查询所有小组请求：URI=/api/student-group");
        try {
            log.info("执行查询所有小组业务");
            List<StudentGroup> studentGroups = studentGroupService.selectAll();
            log.debug("查询所有小组成功：结果数={}", studentGroups.size());
            return Result.success(studentGroups);
        } catch (Exception e) {
            log.error("查询所有小组异常：错误：{}", e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/getByGroupId")
    public Result<StudentGroup> getGroup(@RequestParam Long groupId) {
        log.debug("收到查询小组详情请求：URI=/api/student-group/getByGroupId, 参数：groupId={}", groupId);
        if(groupId == null){
            log.error("参数错误，groupId is null");
            return Result.error(400, "参数错误");
        }
        try {
            log.info("执行查询小组详情业务：groupId={}", groupId);
            StudentGroup studentGroup = studentGroupService.selectByGroupId(groupId);
            if(studentGroup == null){
                log.error("未找到小组：groupId={}", groupId);
                return Result.error(404, "未找到");
            }
            log.debug("查询小组详情成功：groupId={}, groupName={}", groupId, studentGroup.getGroupName());
            return Result.success(studentGroup);
        } catch (Exception e) {
            log.error("查询小组详情异常：groupId={}, 错误：{}", groupId, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    @PostMapping
    @Transactional
    public Result<StudentGroup> create(@RequestParam String groupName,
                                       @RequestParam Long groupLeaderId,
                                       @RequestParam String groupDescription,
                                       @RequestParam List<Long> memberIds) {
        log.info("=== 创建小组请求 ===");
        log.info("groupName: {}", groupName);
        log.info("groupLeaderId: {} (类型：{})", groupLeaderId, groupLeaderId != null ? groupLeaderId.getClass().getName() : "null");
        log.info("groupDescription: {}", groupDescription);
        log.info("memberIds: {}", memberIds);

        try {
            // === 参数校验 ===
            if (groupName == null || groupName.isEmpty()) {
                log.warn("创建小组参数错误：groupName 为空");
                return Result.error(400, "参数错误：groupName 不能为空");
            }
            if (groupLeaderId == null) {
                log.error("创建小组参数错误：groupLeaderId 为 null");
                return Result.error(400, "参数错误：groupLeaderId 不能为空");
            }
            if (groupDescription == null || groupDescription.isEmpty()) {
                log.warn("创建小组参数错误：groupDescription 为空");
                return Result.error(400, "参数错误：groupDescription 不能为空");
            }
            if (memberIds == null || memberIds.isEmpty()) {
                log.warn("创建小组参数错误：memberIds 为空");
                return Result.error(400, "参数错误：memberIds 不能为空");
            }

            // === 1. 验证组长是否存在 ===
            log.info("执行创建小组业务：groupName={}, groupLeaderId={}", groupName, groupLeaderId);
            log.info("正在查询组长，groupLeaderId = {}", groupLeaderId);
            Student leader = studentService.selectById(groupLeaderId);
            log.info("查询结果：leader = {}", leader);
            if (leader == null) {
                log.error("未找到 groupLeaderId={} 的学生记录", groupLeaderId);
                // 尝试查询所有学生，看看数据库中有哪些数据
                List<Student> allStudents = studentService.selectAll();
                log.error("数据库中共有 {} 个学生", allStudents.size());
                if (!allStudents.isEmpty()) {
                    log.error("前 5 个学生的 ID: {}",
                        allStudents.stream().limit(5).map(s -> s.getId()).toList());
                }
                return Result.error(400, "参数错误：groupLeaderId 不存在，ID=" + groupLeaderId);
            }

            // === 2. 组长是否已在其他小组 ===
            GroupMember leaderExisting = groupMemberService.selectById(groupLeaderId);
            if (leaderExisting != null) {
                log.warn("组长已加入其他小组：groupLeaderId={}", groupLeaderId);
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
                log.error("插入分组失败：{}", studentGroup);
                throw new RuntimeException("插入分组失败");
            }

            Long groupId = studentGroup.getGroupId();
            log.info("小组创建成功：groupId={}", groupId);

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
                    log.error("插入组长到 group_members 表失败：groupId={}, leaderId={}", groupId, groupLeaderId);
                    throw new RuntimeException("插入组长成员失败");
                }
                log.info("组长插入成功：groupId={}, leaderId={}", groupId, groupLeaderId);
            } catch (Exception e) {
                log.error("插入组长时发生异常：groupId={}, leaderId={}", groupId, groupLeaderId, e);
                throw new RuntimeException("插入组长成员失败：" + e.getMessage(), e);
            }
            // 更新组长状态为已加入
            leader.setGroupStatus("approval");
            studentService.updateById(leader);
            log.debug("更新组长分组状态：groupLeaderId={}, groupStatus=approval", groupLeaderId);

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
                    log.warn("成员已在其他小组中：memberId={}, name={}", memberId, member.getName());
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
                    log.error("插入成员失败：{}", member.getName());
                    throw new RuntimeException("插入成员失败：" + member.getName());
                }
                // 更新成员状态为已加入
                member.setGroupStatus("approval");
                studentService.updateById(member);
                log.debug("成员插入成功：memberId={}, groupId={}", memberId, groupId);
            }

            log.info("创建小组及成员成功：groupId={}", groupId);
            studentGroup = studentGroupService.selectByGroupId(groupId);
            return Result.success(studentGroup);

        } catch (Exception e) {
            log.error("创建分组异常：groupName={}, groupLeaderId={}, 错误：{}", groupName, groupLeaderId, e.getMessage(), e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    @PutMapping("/{groupId}")
    public Result<StudentGroup> update(@PathVariable("groupId") Long groupId, @RequestBody StudentGroup studentGroup) {
        log.debug("收到更新小组请求：URI=/api/student-group/{}, 参数：groupId={}, group={}", groupId, studentGroup);
        try{
            if (studentGroup == null) {
                log.error("参数错误，studentGroup is null");
                return Result.error(400, "参数错误");
            }
            log.info("执行更新小组业务：groupId={}", groupId);
            studentGroup.setGroupId(groupId);
            StudentGroup group = studentGroupService.selectByGroupId(groupId);
            if (group == null) {
                log.error("用户分组不存在：groupId={}", groupId);
                return Result.error(404, "用户分组不存在!");
            }
            studentGroup.setGroupId(groupId);
            int result = studentGroupService.update(studentGroup);
            if (result > 0) {
                log.debug("更新小组成功：groupId={}", groupId);
                return Result.success(studentGroupService.selectByGroupId(groupId));
            }
            log.error("更新小组失败：groupId={}, studentGroup={}", groupId, studentGroup);
            return Result.error(500, "更新失败");
        } catch (Exception e) {
            log.error("更新学生分组信息时发生异常：groupId={}", groupId, e);
            return Result.error(500, "系统异常，请稍后重试");
        }
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
        log.info("=== 完整更新小组请求 ===");
        log.info("groupId: {}", groupId);
        log.info("请求体：groupName={}, groupDescription={}, groupLeaderId={}, approvalStatus={}",
                body.getGroupName(), body.getGroupDescription(), body.getGroupLeaderId(), body.getApprovalStatus());
        log.info("新增成员：{}", body.getAddMemberIds());
        log.info("移除成员：{}", body.getRemoveMemberIds());

        try {
            log.info("执行完整更新小组业务：groupId={}", groupId);
            StudentGroup group = studentGroupService.selectByGroupId(groupId);
            if (group == null) {
                log.error("小组不存在：groupId={}", groupId);
                return Result.error(404, "用户分组不存在!");
            }

            log.info("当前小组信息：name={}, approvalStatus={}", group.getGroupName(), group.getApprovalStatus());

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
                log.info("小组基础信息更新结果：{}", updateResult);
            }

            // 2) 成员处理（增删）
            if (body.getRemoveMemberIds() != null) {
                for (Long sid : body.getRemoveMemberIds()) {
                    if (sid == null) continue;
                    groupMemberService.deleteMember(sid, groupId);
                    log.info("移除成员：studentId={}", sid);
                }
            }
            if (body.getAddMemberIds() != null && !body.getAddMemberIds().isEmpty()) {
                for (Long sid : body.getAddMemberIds()) {
                    if (sid == null) continue;
                    log.info("添加成员：studentId={}", sid);
                    // 若该学生已在任一小组，禁止重复加入（与创建逻辑一致）
                    GroupMember existing = groupMemberService.selectById(sid);
                    if (existing != null) {
                        log.warn("成员已在其他小组中：studentId={}", sid);
                        return Result.error(400, "成员已在其他小组中：" + sid);
                    }
                    // 填充成员姓名/班级
                    Student member = studentService.selectById(sid);
                    if (member == null) {
                        log.warn("成员不存在：studentId={}", sid);
                        return Result.error(400, "成员不存在：" + sid);
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
                    log.info("成员添加成功：{}", item);
                    // 同步更新学生的分组状态为 approval
                    try {
                        member.setGroupStatus("approval");
                        studentService.updateById(member);
                    } catch (Exception ignore) {}
                }
            }

            log.info("完整更新小组成功：groupId={}", groupId);
            return Result.success(studentGroupService.selectByGroupId(groupId));
        } catch (Exception e) {
            log.error("批量更新学生分组信息异常：groupId={}", groupId, e);
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
        log.debug("收到按审批状态查询小组请求：URI=/api/student-group/approvalStatus, 参数：approvalStatus={}", approvalStatus);
        try {
            log.info("执行按审批状态查询小组业务：approvalStatus={}", approvalStatus);
            List<StudentGroup> studentGroups;
            if (approvalStatus == null) {
                log.debug("审批状态为空，查询所有小组");
                studentGroups = studentGroupService.selectAll();
            } else {
                studentGroups = studentGroupService.selectByApprovalStatus(approvalStatus);
            }
            log.debug("按审批状态查询小组成功：approvalStatus={}, 结果数={}", approvalStatus, studentGroups.size());
            return Result.success(studentGroups);
        } catch (Exception e) {
            log.error("查询学生分组信息时发生异常：approvalStatus={}", approvalStatus, e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    @DeleteMapping("/{groupId}")
    public Result<String> delete(@PathVariable("groupId") Long groupId)  {
        log.debug("收到删除小组请求：URI=/api/student-group/{}, 参数：groupId={}", groupId, groupId);
        try {
            log.info("执行删除小组业务：groupId={}", groupId);
            int result = studentGroupService.deleteByGroupId(groupId);
            if (result > 0) {
                log.debug("删除小组成功：groupId={}", groupId);
                return Result.success("删除成功");
            }
            log.warn("未找到小组：groupId={}", groupId);
            return Result.error(404, "未找到指定的学生分组");
        } catch (Exception e) {
            log.error("删除学生分组信息时发生异常：groupId={}", groupId, e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }
}
