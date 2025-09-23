package com.ccut.service;

import com.ccut.entity.GroupMember;

public interface GroupMemberService {


    int insertMember(GroupMember groupMember);
    int deleteMember(Long id);
}
