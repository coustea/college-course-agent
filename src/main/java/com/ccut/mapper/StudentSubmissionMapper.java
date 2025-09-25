package com.ccut.mapper;

import com.ccut.entity.StudentSubmission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentSubmissionMapper {

    int insert(StudentSubmission studentSubmission);

}
