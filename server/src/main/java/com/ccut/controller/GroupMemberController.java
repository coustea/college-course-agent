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
        log.debug("收到查询小组成员请求：URI=/api/groupMember/getById, 参数：studentId={}", studentId);
        System.out.println(studentId);
        if(studentId == null){
            log.error("参数错误，studentId is null");
            return Result.error(400, "参数错误");
        }
        try {
            log.info("执行查询小组成员业务：studentId={}", studentId);
            GroupMember groupMember = groupMemberService.selectById(studentId);
            // 查询不到数据时返回成功响应，数据为 null
            log.debug("查询小组成员成功：studentId={}, groupId={}", studentId, groupMember != null ? groupMember.getGroupId() : null);
            return Result.success(groupMember);
        } catch (Exception e) {
            log.error("查询小组成员异常：studentId={}, 错误：{}", studentId, e.getMessage(), e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<GroupMember> insert(@RequestBody GroupMember groupMember) {
        log.debug("收到插入小组成员请求：URI=/api/groupMember, 参数：groupMember={}", groupMember);
        if(groupMember == null){
            log.error("参数错误，groupMember is null");
            return Result.error(400, "参数错误");
        }
        try {
            if (groupMember.getRole() == null){
                groupMember.setRole(GroupMember.GroupMemberRole.member);
                log.debug("角色未设置，默认为 member");
            }
            if (groupMember.getJoinStatus() == null) {
                groupMember.setJoinStatus(GroupMember.Status.approval);
                log.debug("加入状态未设置，默认为 approval");
            }
            
            log.info("执行插入小组成员业务：studentId={}, groupId={}", groupMember.getStudentId(), groupMember.getGroupId());
            Student student = studentService.selectById(groupMember.getStudentId());
            if(student == null){
                log.error("参数错误，student is null, studentId={}", groupMember.getStudentId());
                return Result.error(400, "参数错误");
            }
            groupMember.setStudentName(student.getName());
            groupMember.setStudentNumber(student.getStudentNumber());
            
            int res = groupMemberService.insertMember(groupMember);
            if(res > 0){
                log.debug("插入小组成员成功：studentId={}, groupId={}, role={}", 
                        groupMember.getStudentId(), groupMember.getGroupId(), groupMember.getRole());
                return Result.success(groupMember);
            }
            log.error("插入小组成员失败：groupMember={}", groupMember);
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            log.error("插入小组成员异常：groupMember={}, 错误：{}", groupMember, e.getMessage(), e);
            return Result.error(500, "添加失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/{studentId}")
    public Result<String> delete(@PathVariable Long studentId, @PathVariable Long groupId) {
        log.debug("收到删除小组成员请求：URI=/api/groupMember/{}/{}, 参数：studentId={}, groupId={}", groupId, studentId, studentId, groupId);
        if(studentId == null || groupId == null){
            log.error("参数错误，studentId is {}, groupId is {}", studentId, groupId);
            return Result.error(400, "参数错误");
        }

        try {
            log.info("执行删除小组成员业务：studentId={}, groupId={}", studentId, groupId);
            int res = groupMemberService.deleteMember(studentId, groupId);
            if(res > 0){
                try {
                    // 同步更新学生表的分组状态为 pending（待申请）
                    Student stu = studentService.selectById(studentId);
                    if (stu != null) {
                        stu.setGroupStatus("pending");
                        studentService.updateById(stu);
                        log.debug("同步更新学生分组状态：studentId={}, groupStatus=pending", studentId);
                    }
                } catch (Exception e) {
                    log.warn("删除成员后更新学生 group_status 失败：studentId={}, err={}", studentId, e.getMessage());
                }
                log.debug("删除小组成员成功：studentId={}, groupId={}", studentId, groupId);
                return Result.success("删除成功");
            }
            log.error("删除小组成员失败：studentId={}, groupId={}", studentId, groupId);
            return Result.error(500, "删除失败");
        } catch (Exception e) {
            log.error("删除小组成员异常：studentId={}, groupId={}, 错误：{}", studentId, groupId, e.getMessage(), e);
            return Result.error(500, "删除失败：" + e.getMessage());
        }
    }
}
