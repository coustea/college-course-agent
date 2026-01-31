package com.ccut.service;

import com.ccut.entity.Course;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程服务接口
 */
public interface CourseService {

    /**
     * 插入课程
     * @param course 课程信息
     * @return 插入行数
     */
    int insert(Course course);

    /**
     * 插入课程（带图片上传）
     * @param course 课程信息
     * @param image 图片文件（可选）
     * @return 插入后的课程
     */
    Course insertWithImage(Course course, MultipartFile image);

    /**
     * 删除课程
     * @param courseId 课程ID
     * @return 删除行数
     */
    int deleteById(Long courseId);

    /**
     * 更新课程
     * @param course 课程信息
     * @return 更新行数
     */
    int updateById(Course course);

    /**
     * 更新课程（带图片上传）
     * @param course 课程信息
     * @param image 图片文件（可选）
     * @return 更新结果消息
     */
    String updateWithImage(Course course, MultipartFile image);

    /**
     * 查询所有课程
     * @return 课程列表
     */
    List<Course> selectAll();

    /**
     * 根据ID查询课程
     * @param courseId 课程ID
     * @return 课程信息
     */
    Course selectById(Long courseId);

    /**
     * 按名称搜索课程
     * @param name 课程名称
     * @return 课程列表
     */
    List<Course> searchByName(String name);

    /**
     * 搜索课程
     * @param name 课程名称（可选）
     * @param description 课程描述（可选）
     * @return 课程列表
     */
    List<Course> search(String name, String description);

}


