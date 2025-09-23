package com.ccut.service.Impl;

import com.ccut.entity.GroupMember;
import com.ccut.mapper.GroupMemberMapper;
import com.ccut.service.GroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    public int deleteMember(Long studentId, Long groupId) {
        return groupMemberMapper.deleteMember(studentId, groupId);
    }

    @Override
    public int insertMember(GroupMember groupMember) {
        return groupMemberMapper.insertMember(groupMember);
    }
}
