package com.ccut.mapper;

import com.ccut.dto.TeacherCourseCard;
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
    Course selectByCourseCode(@org.apache.ibatis.annotations.Param("courseCode") String courseCode);
    List<Course> search(
            @org.apache.ibatis.annotations.Param("name") String name,
            @org.apache.ibatis.annotations.Param("description") String description);

    // 新增：按教师ID返回课程卡片字段
    List<TeacherCourseCard> listCourseCardsByTeacher(@org.apache.ibatis.annotations.Param("teacherId") Long teacherId);

    /**
     * 更新课程发布状态
     * @param courseId 课程ID
     * @param publishStatus 发布状态 (draft/published)
     * @return 更新行数
     */
    int updatePublishStatus(@org.apache.ibatis.annotations.Param("courseId") Long courseId,
                           @org.apache.ibatis.annotations.Param("publishStatus") String publishStatus);

    /**
     * 根据教师ID查询课程
     * @param teacherId 教师ID
     * @return 课程列表
     */
    List<Course> selectByTeacherId(@org.apache.ibatis.annotations.Param("teacherId") Long teacherId);

    /**
     * 根据发布状态查询课程
     * @param publishStatus 发布状态 (draft/published)
     * @return 课程列表
     */
    List<Course> selectByPublishStatus(@org.apache.ibatis.annotations.Param("publishStatus") String publishStatus);
}

