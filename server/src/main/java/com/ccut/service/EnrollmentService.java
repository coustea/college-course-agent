package com.ccut.service;

import com.ccut.entity.Enrollment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 选课服务接口
 */
public interface EnrollmentService {

    /**
     * 学生选课
     * 检查：1) 课程已发布 2) 学生未选过该课程
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课记录
     */
    Enrollment enroll(Long studentId, Long courseId);

    /**
     * 学生退课
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 是否成功
     */
    boolean drop(Long studentId, Long courseId);

    /**
     * 教师批量添加学生到课程
     * @param courseId 课程ID
     * @param teacherId 教师ID（用于权限验证）
     * @param studentIds 学生ID列表
     * @return 成功添加的学生数量
     */
    int batchEnroll(Long courseId, Long teacherId, List<Long> studentIds);

    /**
     * 通过Excel批量导入学生到课程
     * @param courseId 课程ID
     * @param teacherId 教师ID（用于权限验证）
     * @param file Excel文件（包含学号列表）
     * @return 导入结果 {success: 成功数, failed: 失败数, notFound: 未找到学号列表, details: 详细信息}
     */
    Map<String, Object> importStudentsFromExcel(Long courseId, Long teacherId, MultipartFile file);

    /**
     * 查询学生的所有已选课程
     * @param studentId 学生ID
     * @return 选课记录列表
     */
    List<Enrollment> getEnrollmentsByStudent(Long studentId);

    /**
     * 查询课程的所有选课学生
     * @param courseId 课程ID
     * @return 选课记录列表
     */
    List<Enrollment> getEnrollmentsByCourse(Long courseId);

    /**
     * 检查学生是否已选某课程
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 是否已选
     */
    boolean isEnrolled(Long studentId, Long courseId);
}
