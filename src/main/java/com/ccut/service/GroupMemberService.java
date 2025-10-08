package com.ccut.service;

import com.ccut.entity.GroupMember;

public interface GroupMemberService {

    GroupMember selectById(Long id);
    int insertMember(GroupMember groupMember);
    int deleteMember(Long studentId, Long groupId);

}
