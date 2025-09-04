package com.ccut.common.service;

import com.ccut.common.entity.LearningProgress;
import com.ccut.common.mapper.LearningProgressMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LearningProgressService {

    @Resource
    LearningProgressMapper learningProgressMapper;

    //添加学习进度记录
    public int addLearningProgress(LearningProgress learningProgress) {
        return learningProgressMapper.insert(learningProgress);
    }
    //更新学习进度记录
    public int updateLearningProgress(LearningProgress learningProgress) {
        return learningProgressMapper.updateById(learningProgress);

    }
    //查看学习进度信息、课程名称、教师姓名
    public List<LearningProgress> findByStudentId(Long studentId) {
        return learningProgressMapper.findByStudentId(studentId);
    }
    public Optional<LearningProgress> findByStudentIdAndCourseId(Long studentId, Long courseId) {
        return learningProgressMapper.findByStudentIdAndCourseId(studentId, courseId);
    }
}
