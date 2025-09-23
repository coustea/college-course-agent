package com.ccut.mapper;

import com.ccut.entity.StudentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentGroupMapper {

    int insert(StudentGroup studentGroup);
    int update(StudentGroup studentGroup);
    List<StudentGroup> selectPending();
}


