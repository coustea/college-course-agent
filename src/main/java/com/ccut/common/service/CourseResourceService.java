package com.ccut.common.service;

import com.ccut.common.entity.CourseResource;
import com.ccut.common.mapper.CourseResourceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseResourceService {

    @Resource
    CourseResourceMapper courseResourceMapper;
    public int addCourseResource(CourseResource courseResource) {
        return courseResourceMapper.insert(courseResource);
    }
    public int deleteCourseResource(Integer id) {
        return courseResourceMapper.deleteById(id);
    }
    public int updateCourseResource(CourseResource courseResource) {
       return  courseResourceMapper.updateById(courseResource);
    }
    public List<CourseResource> selectById(Integer id){
        return courseResourceMapper.selectById(id);
    }



}
