package com.ccut.controller;

import com.ccut.entity.GroupMember;
import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
import com.ccut.service.Impl.GroupMemberServiceImpl;
import com.ccut.service.Impl.StudentGroupServiceImpl;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.Impl.TeacherServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
    public Result<StudentGroup> create(@RequestParam String groupName,
                                       @RequestParam Long groupLeaderId,
                                       @RequestParam String groupDescription,
                                       @RequestParam List<Long> memberIds) {
        try {
            // === 参数校验 ===
            if (groupName == null || groupName.isEmpty()) {
                return Result.error(400, "参数错误：groupName 不能为空");
            }
            if (groupLeaderId == null) {
                return Result.error(400, "参数错误：groupLeaderId 不能为空");
            }
            if (groupDescription == null || groupDescription.isEmpty()) {
                return Result.error(400, "参数错误：groupDescription 不能为空");
            }
            if (memberIds == null || memberIds.isEmpty()) {
                return Result.error(400, "参数错误：memberIds 不能为空");
            }

            // === 1. 验证组长是否存在 ===
            Student leader = studentService.selectById(groupLeaderId);
            if (leader == null) {
                return Result.error(400, "参数错误：groupLeaderId 不存在");
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
                return Result.error(500, "插入分组失败");
            }

            Long groupId = studentGroup.getGroupId();
            log.info("小组创建成功，groupId={}", groupId);

            // === 4. 插入组长 ===
            GroupMember leaderMember = new GroupMember(
                    groupId,
                    groupLeaderId,
                    leader.getName(),
                    leader.getClassName(),
                    GroupMember.GroupMemberRole.leader,
                    GroupMember.Status.approved
            );
            int leaderInsert = groupMemberService.insertMember(leaderMember);
            if (leaderInsert <= 0) {
                throw new RuntimeException("插入组长成员失败");
            }
            // 更新组长状态为已加入
            leader.setGroupStatus("approved");
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
                        member.getClassName(),
                        GroupMember.GroupMemberRole.member,
                        GroupMember.Status.approved
                );
                int memberInsert = groupMemberService.insertMember(memberEntry);
                if (memberInsert <= 0) {
                    throw new RuntimeException("插入成员失败: " + member.getName());
                }
                // 更新成员状态为已加入
                member.setGroupStatus("approved");
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