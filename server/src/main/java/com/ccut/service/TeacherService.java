package com.ccut.service;

import com.ccut.dto.TeacherCourseCard;
import com.ccut.dto.TeacherVideoItem;
import com.ccut.entity.Course;
import com.ccut.entity.Student;
import com.ccut.entity.Teacher;

import java.util.List;
import java.util.Map;

/**
 * 教师服务接口
 */
public interface TeacherService {

    /**
     * 插入教师
     * @param teacher 教师信息
     * @return 插入行数
     */
    int insert(Teacher teacher);

    /**
     * 更新教师
     * @param teacher 教师信息
     * @return 更新行数
     */
    int update(Teacher teacher);

    /**
     * 删除教师
     * @param id 教师ID
     * @return 删除行数
     */
    int deleteById(Long id);

    /**
     * 查询所有教师
     * @return 教师列表
     */
    List<Teacher> selectAll();

    /**
     * 根据ID查询教师
     * @param id 教师ID
     * @return 教师信息
     */
    Teacher selectById(Long id);

    /**
     * 获取教师的班级列表
     * @param teacherId 教师ID
     * @return 班级名称列表
     */
    List<String> selectClassNameByTeacherId(Long teacherId);

    /**
     * 学生选课
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课结果消息
     */
    String enrollStudent(Long studentId, Long courseId);

    /**
     * 按学生ID查询已加入的课程
     * @param studentId 学生ID
     * @return 课程列表
     */
    List<Course> findCoursesByStudentId(Long studentId);

    /**
     * 按课程ID查询参与课程的学生
     * @param courseId 课程ID
     * @return 学生列表
     */
    List<Student> findStudentsByCourseId(Long courseId);

    /**
     * 按教师ID返回其课程下的视频列表
     * @param teacherId 教师ID
     * @return 包含视频和课程卡片的数据
     */
    Map<String, Object> listTeacherVideos(Long teacherId);

}
