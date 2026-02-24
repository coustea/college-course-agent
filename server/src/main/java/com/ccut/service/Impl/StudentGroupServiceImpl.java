package com.ccut.service.Impl;

import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
import com.ccut.service.StudentGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学生小组服务实现类
 */
@Service
public class StudentGroupServiceImpl implements StudentGroupService {

    private static final Logger log = LoggerFactory.getLogger(StudentGroupServiceImpl.class);

    @Autowired
    private StudentGroupMapper studentGroupMapper;

    @Override
    public int insert(StudentGroup studentGroup) {
        log.debug("执行方法：insert, 参数：studentGroup={}", studentGroup != null ? "groupName=" + studentGroup.getGroupName() : "null");
        try {
            int result = studentGroupMapper.insert(studentGroup);
            log.info("学生小组插入成功：groupId={}, groupName={}, result={}", 
                    studentGroup != null ? studentGroup.getGroupId() : "null",
                    studentGroup != null ? studentGroup.getGroupName() : "null", 
                    result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("学生小组插入失败：groupName={}, error={}", studentGroup != null ? studentGroup.getGroupName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int update(StudentGroup studentGroup) {
        log.debug("执行方法：update, 参数：studentGroup={}", studentGroup != null ? "groupId=" + studentGroup.getGroupId() : "null");
        try {
            int result = studentGroupMapper.update(studentGroup);
            log.info("学生小组更新成功：groupId={}, result={}", studentGroup != null ? studentGroup.getGroupId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("学生小组更新失败：groupId={}, error={}", studentGroup != null ? studentGroup.getGroupId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<StudentGroup> selectByApprovalStatus(StudentGroup.GroupApprovalStatus approvalStatus) {
        log.debug("执行方法：selectByApprovalStatus, 参数：approvalStatus={}", approvalStatus);
        try {
            List<StudentGroup> result = studentGroupMapper.selectByApprovalStatus(approvalStatus);
            log.info("按审批状态查询学生小组成功：approvalStatus={}, count={}", approvalStatus, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("按审批状态查询学生小组失败：approvalStatus={}, error={}", approvalStatus, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<StudentGroup> selectAll() {
        log.debug("执行方法：selectAll");
        try {
            List<StudentGroup> result = studentGroupMapper.selectAll();
            log.info("查询所有学生小组成功：count={}", result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询所有学生小组失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public StudentGroup selectByGroupId(Long groupId) {
        log.debug("执行方法：selectByGroupId, 参数：groupId={}", groupId);
        try {
            StudentGroup result = studentGroupMapper.selectByGroupId(groupId);
            log.debug("方法返回：result={}", result != null ? "groupId=" + result.getGroupId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询学生小组失败：groupId={}, error={}", groupId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int deleteByGroupId(Long groupId) {
        log.debug("执行方法：deleteByGroupId, 参数：groupId={}", groupId);
        try {
            int result = studentGroupMapper.deleteByGroupId(groupId);
            log.info("学生小组删除成功：groupId={}, result={}", groupId, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("学生小组删除失败：groupId={}, error={}", groupId, e.getMessage(), e);
            throw e;
        }
    }
}
