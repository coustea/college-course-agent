package com.ccut.service.Impl;

import com.ccut.entity.GroupMember;
import com.ccut.mapper.GroupMemberMapper;
import com.ccut.service.GroupMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 小组成员服务实现类
 */
@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    private static final Logger log = LoggerFactory.getLogger(GroupMemberServiceImpl.class);

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    public int deleteMember(Long studentId, Long groupId) {
        log.debug("执行方法：deleteMember, 参数：studentId={}, groupId={}", studentId, groupId);
        try {
            int result = groupMemberMapper.deleteMember(studentId, groupId);
            log.info("删除小组成员成功：studentId={}, groupId={}, result={}", studentId, groupId, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("删除小组成员失败：studentId={}, groupId={}, error={}", studentId, groupId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GroupMember selectById(Long id) {
        log.debug("执行方法：selectById, 参数：id={}", id);
        try {
            GroupMember result = groupMemberMapper.selectById(id);
            log.debug("方法返回：result={}", result != null ? "id=" + result.getId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询小组成员失败：id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int insertMember(GroupMember groupMember) {
        log.debug("执行方法：insertMember, 参数：groupMember={}", groupMember != null ? "studentId=" + groupMember.getStudentId() : "null");
        try {
            int result = groupMemberMapper.insertMember(groupMember);
            log.info("插入小组成员成功：studentId={}, groupId={}, result={}", 
                    groupMember != null ? groupMember.getStudentId() : "null",
                    groupMember != null ? groupMember.getGroupId() : "null", 
                    result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("插入小组成员失败：studentId={}, groupId={}, error={}", 
                    groupMember != null ? groupMember.getStudentId() : "null",
                    groupMember != null ? groupMember.getGroupId() : "null", 
                    e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<GroupMember> selectByGroupId(Long groupId) {
        log.debug("执行方法：selectByGroupId, 参数：groupId={}", groupId);
        try {
            List<GroupMember> result = groupMemberMapper.selectByGroupId(groupId);
            log.info("查询小组成员成功：groupId={}, count={}", groupId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询小组成员失败：groupId={}, error={}", groupId, e.getMessage(), e);
            throw e;
        }
    }
}
