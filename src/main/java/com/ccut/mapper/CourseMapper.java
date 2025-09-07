package com.ccut.mapper;

import com.ccut.entity.Course;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseMapper {
    int insert(Course course);
    int updateById(Course course);
    int deleteById(Long courseId);
    List<Course> selectAll();
    Course selectById(Long courseId);
}

package com.ccut.mapper;

import com.ccut.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseMapper {


    int insertCourse(@Param("course") Course course);
}
