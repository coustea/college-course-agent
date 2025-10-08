package com.ccut.mapper;

import com.ccut.entity.StudentPersonalSubmission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentPersonalSubmissionMapper {

    int insert(StudentPersonalSubmission submission);

    int upsert(StudentPersonalSubmission submission);
}


