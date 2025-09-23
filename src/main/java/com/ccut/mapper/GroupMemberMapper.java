package com.ccut.mapper;

import com.ccut.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GroupMemberMapper {

    int insertMember(GroupMember groupMember);
    int deleteMember(@Param("studentId") Long studentId, @Param("groupId") Long groupId);
}
