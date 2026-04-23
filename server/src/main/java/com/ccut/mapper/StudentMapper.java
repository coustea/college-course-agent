package com.ccut.mapper;

import com.ccut.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

    int insertStudent(Student student);
    Student getStudentByStudentNumber(String studentNumber);
    int updateById(Student student);
    int deleteById(Long id);
    List<Student> selectAll();
    Student selectById(Long id);
    List<Student> selectByGrade(String grade);
    List<Student> selectByClassName(String className);

    // 推荐系统需要的辅助方法
    List<Long> findAllStudentIds();

    /**
     * 根据学号列表批量查询学生
     * @param studentNumbers 学号列表
     * @return 学生列表
     */
    List<Student> findByStudentNumbers(@Param("studentNumbers") List<String> studentNumbers);
}
