package com.ccut.controller;

import com.ccut.entity.GroupMember;
import com.ccut.dto.Result;
import com.ccut.entity.Student;
import com.ccut.service.Impl.GroupMemberServiceImpl;
import com.ccut.service.Impl.StudentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 小组成员控制器
 */
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
            throw new IllegalArgumentException("参数错误");
        }
        GroupMember groupMember = groupMemberService.selectById(studentId);
        return Result.success(groupMember);
    }

    @PostMapping
    public Result<GroupMember> insert(@RequestBody GroupMember groupMember) {
        if(groupMember == null){
            throw new IllegalArgumentException("参数错误");
        }
        if (groupMember.getRole() == null){
            groupMember.setRole(GroupMember.GroupMemberRole.member);
        }
        if (groupMember.getJoinStatus() == null) {
            groupMember.setJoinStatus(GroupMember.Status.approval);
        }

        Student student = studentService.selectById(groupMember.getStudentId());
        if(student == null){
            throw new IllegalArgumentException("参数错误");
        }
        groupMember.setStudentName(student.getName());
        groupMember.setStudentNumber(student.getStudentNumber());

        int res = groupMemberService.insertMember(groupMember);
        if(res <= 0){
            throw new RuntimeException("添加失败");
        }
        return Result.success(groupMember);
    }

    @DeleteMapping("/{groupId}/{studentId}")
    public Result<String> delete(@PathVariable Long studentId, @PathVariable Long groupId) {
        if(studentId == null || groupId == null){
            throw new IllegalArgumentException("参数错误");
        }

        int res = groupMemberService.deleteMember(studentId, groupId);
        if(res > 0){
            try {
                Student stu = studentService.selectById(studentId);
                if (stu != null) {
                    stu.setGroupStatus("pending");
                    studentService.updateById(stu);
                }
            } catch (Exception e) {
                log.warn("删除成员后更新学生 group_status 失败：studentId={}, err={}", studentId, e.getMessage());
            }
            return Result.success("删除成功");
        }
        throw new RuntimeException("删除失败");
    }
}
