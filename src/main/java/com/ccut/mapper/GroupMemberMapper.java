package com.ccut.mapper;

import com.ccut.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMemberMapper {

    int insertMember(GroupMember groupMember);
    int deleteMember(Long id);
}
