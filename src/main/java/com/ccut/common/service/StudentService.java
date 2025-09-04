package com.ccut.common.service;

import com.ccut.common.entity.Student;
import com.ccut.common.entity.User;
import com.ccut.common.exception.CustomerException;
import com.ccut.common.mapper.StudentMapper;
import com.ccut.common.Utils.TakenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    @Resource
    StudentMapper studentMapper;

    //学生登陆
    public Student login(User user) {
        Student dbStudent = studentMapper.selectByUsername(user.getUsername());
        if(dbStudent == null){
            throw new CustomerException("账号不存在！");
        }

        if(!dbStudent.getPassword().equals(user.getPassword())){
            throw new CustomerException("账号或密码错误");
        }

        String token = TakenUtils.createToken(dbStudent.getId() + "-" + "STUDENT", dbStudent.getPassword());
        dbStudent.setToken(token);
        // 写回数据库
        studentMapper.updateById(dbStudent);
        return dbStudent;
    }
    //添加学生
    public int addStudent(Student student) {
        return studentMapper.insert(student);
    }
    //删除学生
    public int deleteStudent(Integer id) {

        return studentMapper.deleteById(id);
    }
    //修改学生
    public int updateStudent(Student student) {
       return studentMapper.updateById(student);

    }
    //查询
    public List<Student> selectAll(Student student) {
        return studentMapper.selectAll(student);
    }

}
