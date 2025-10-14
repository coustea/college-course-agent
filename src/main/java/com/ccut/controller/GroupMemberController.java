package com.ccut.controller;

import com.ccut.entity.GroupMember;
import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.service.Impl.GroupMemberServiceImpl;
import com.ccut.service.Impl.StudentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/groupMember")
public class GroupMemberController {

    @Autowired
    private GroupMemberServiceImpl groupMemberService;
    @Autowired
    private StudentServiceImpl studentService;

    @PostMapping("/getById")
    public Result<GroupMember> getGroupMember(@RequestParam Long studentId) {
        System.out.println(studentId);
        if(studentId == null){
            log.error("参数错误，studentId is null");
            return Result.error(400, "参数错误");
        }
        GroupMember groupMember = groupMemberService.selectById(studentId);
        if(groupMember == null){
            log.error("未找到，studentId is {}", studentId);
            return Result.error(404, "未找到");
        }
        return Result.success(groupMember);
    }

    @PostMapping
    public Result<GroupMember> insert(@RequestBody GroupMember groupMember) {
        if(groupMember == null){
            log.error("参数错误，groupMember is null");
            return Result.error(400, "参数错误");
        }
        if (groupMember.getRole() == null){
            groupMember.setRole(GroupMember.GroupMemberRole.member);
        }
        if (groupMember.getJoinStatus() == null) {
            groupMember.setJoinStatus(GroupMember.Status.approval);
        }
        Student student = studentService.selectById(groupMember.getStudentId());
        if(student == null){
            log.error("参数错误，student is null");
            return Result.error(400, "参数错误");
        }
        groupMember.setStudentName(student.getName());
        int res = groupMemberService.insertMember(groupMember);
        if(res > 0){
            return Result.success(groupMember);
        }
        log.error("添加失败，groupMember is {}", groupMember);
        return Result.error(500, "添加失败");
    }

    @DeleteMapping("/{groupId}/{studentId}")
    public Result<String> delete(@PathVariable Long studentId, @PathVariable Long groupId) {
        if(studentId == null || groupId == null){
            log.error("参数错误，studentId is {}, groupId is {}", studentId, groupId);
            return Result.error(400, "参数错误");
        }

        int res = groupMemberService.deleteMember(studentId, groupId);
        if(res > 0){
            try {
                // 同步更新学生表的分组状态为 pending（待申请）
                Student stu = studentService.selectById(studentId);
                if (stu != null) {
                    stu.setGroupStatus("pending");
                    studentService.updateById(stu);
                }
            } catch (Exception e) {
                log.warn("删除成员后更新学生group_status失败: studentId={}, err={}", studentId, e.getMessage());
            }
            return Result.success("删除成功");
        }
        log.error("删除失败，studentId is {}", studentId);
        return Result.error(500, "删除失败");
    }
}
