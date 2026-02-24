package com.ccut.service.Impl;

import com.ccut.entity.Student;
import com.ccut.mapper.StudentMapper;
import com.ccut.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学生服务实现类
 */
@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    private StudentMapper studentMapper;


    @Override
    public Student getStudentByStudentNumber(String studentNumber) {
        log.debug("执行方法：getStudentByStudentNumber, 参数：studentNumber={}", studentNumber);
        try {
            Student result = studentMapper.getStudentByStudentNumber(studentNumber);
            log.debug("方法返回：result={}", result != null ? "id=" + result.getId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询学生失败：studentNumber={}, error={}", studentNumber, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int updateById(Student student) {
        log.debug("执行方法：updateById, 参数：student={}", student != null ? "id=" + student.getId() : "null");
        try {
            int result = studentMapper.updateById(student);
            log.info("学生信息更新成功：studentId={}, result={}", student != null ? student.getId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("学生信息更新失败：studentId={}, error={}", student != null ? student.getId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(Student student) {
        log.debug("执行方法：insert, 参数：student={}", student != null ? "name=" + student.getName() : "null");
        try {
            int result = studentMapper.insertStudent(student);
            log.info("学生插入成功：studentId={}, studentNumber={}, result={}", 
                    student != null ? student.getId() : "null",
                    student != null ? student.getStudentNumber() : "null", 
                    result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("学生插入失败：studentNumber={}, error={}", student != null ? student.getStudentNumber() : "null", e.getMessage(), e);
            throw e;
        }
    }

    public int deleteById(Long id) {
        log.debug("执行方法：deleteById, 参数：id={}", id);
        try {
            int result = studentMapper.deleteById(id);
            log.info("学生删除成功：studentId={}, result={}", id, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("学生删除失败：studentId={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    public java.util.List<Student> selectAll() {
        log.debug("执行方法：selectAll");
        try {
            List<Student> result = studentMapper.selectAll();
            log.info("查询所有学生成功：count={}", result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询所有学生失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Student selectById(Long id) {
        log.debug("执行方法：selectById, 参数：id={}", id);
        try {
            Student result = studentMapper.selectById(id);
            log.debug("方法返回：result={}", result != null ? "id=" + result.getId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询学生失败：studentId={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public java.util.List<Student> selectByGrade(String grade) {
        log.debug("执行方法：selectByGrade, 参数：grade={}", grade);
        try {
            List<Student> result = studentMapper.selectByGrade(grade);
            log.info("按年级查询学生成功：grade={}, count={}", grade, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("按年级查询学生失败：grade={}, error={}", grade, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Student> selectByClassName(String className) {
        log.debug("执行方法：selectByClassName, 参数：className={}", className);
        try {
            List<Student> result = studentMapper.selectByClassName(className);
            log.info("按班级查询学生成功：className={}, count={}", className, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("按班级查询学生失败：className={}, error={}", className, e.getMessage(), e);
            throw e;
        }
    }

}
