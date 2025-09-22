package com.ccut.service.Impl;

import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
import com.ccut.service.StudentGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {

    @Autowired
    private StudentGroupMapper studentGroupMapper;

    @Override
    public int insert(StudentGroup studentGroup) {
        return studentGroupMapper.insert(studentGroup);
    }

    @Override
    public int update(StudentGroup studentGroup) {
        return studentGroupMapper.update(studentGroup);
    }

    @Override
    public List<StudentGroup> selectPending() {
        return studentGroupMapper.selectPending();
    }
}
