package com.ccut.controller;

import com.ccut.entity.GroupMember;
import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
import com.ccut.service.Impl.GroupMemberServiceImpl;
import com.ccut.service.Impl.StudentGroupServiceImpl;
import com.ccut.service.Impl.StudentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/insert")
    public Result<StudentGroup> insert(@RequestBody StudentGroup studentGroup) {
        try {
            if (studentGroup == null) {
                log.error("参数错误，studentGroup is null");
                return Result.error(400, "参数错误");
            }
            Student student = studentService.selectById(studentGroup.getGroupLeaderId());
            if (student == null) {
                log.error("参数错误，groupLeaderId is not exist");
                return Result.error(400, "参数错误，groupLeaderId is not exist");
            }
            if (studentGroup.getApprovalStatus() == null) {
                studentGroup.setApprovalStatus(StudentGroup.GroupApprovalStatus.approved);
            }
            if (studentGroup.getStatus() == null) {
                studentGroup.setStatus(StudentGroup.GroupStatus.active);
            }
            int result = studentGroupService.insert(studentGroup);
            if (result > 0) {
                log.info("插入成功，studentGroupId is {}", studentGroup.getGroupId());
                GroupMember groupMember = new GroupMember(
                    studentGroup.getGroupId(),
                    studentGroup.getCourseId(),
                    studentGroup.getGroupLeaderId(),
                    student.getName(),
                    GroupMember.GroupMemberRole.leader,
                    GroupMember.Status.approved
                );
                int res = groupMemberService.insertMember(groupMember);
                return Result.success(studentGroup);
            }
            log.error("插入失败，studentGroup is {}", studentGroup);
            return Result.error(500, "插入失败");
        } catch (Exception e) {
            log.error("插入学生分组信息时发生异常: ", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody StudentGroup studentGroup) {
        try{
            if (studentGroup == null) {
                log.error("参数错误，studentGroup is null");
                return Result.error(400, "参数错误");
            }

            int result = studentGroupService.update(studentGroup);
            if (result > 0) {
                return Result.success("更新成功");
            }
            log.error("更新失败，studentGroup is {}", studentGroup);
            return Result.error(500, "更新失败");
        } catch (Exception e) {
            log.error("更新学生分组信息时发生异常: ", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    @GetMapping("/pending")
    public Result<List<StudentGroup>> selectPending() {
        try {
            List<StudentGroup> studentGroups = studentGroupService.selectPending();
            return Result.success(studentGroups);
        } catch (Exception e) {
            log.error("查询待审核学生分组信息时发生异常: ", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }
}


