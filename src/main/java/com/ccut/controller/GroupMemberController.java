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
            groupMember.setJoinStatus(GroupMember.Status.approved);
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
            return Result.success("删除成功");
        }
        log.error("删除失败，studentId is {}", studentId);
        return Result.error(500, "删除失败");
    }
}
