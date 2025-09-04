package com.ccut.common.service;

import com.ccut.common.entity.Teacher;
import com.ccut.common.entity.User;
import com.ccut.common.exception.CustomerException;
import com.ccut.common.mapper.CourseMapper;
import com.ccut.common.mapper.TeacherMapper;
import com.ccut.common.Utils.TakenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherService {

    @Resource
    TeacherMapper teacherMapper;
    @Resource
    CourseMapper courseMapper;

    //教师登陆
    public Teacher login(User user) {
        Teacher dbTeacher = teacherMapper.selectByUsername(user.getUsername());
        if(dbTeacher == null){
            throw new CustomerException("账号不存在！");
        }

        if(!dbTeacher.getPassword().equals(user.getPassword())){
            throw new CustomerException("账号或密码错误");
        }

        String token = TakenUtils.createToken(dbTeacher.getId() + "-" + "TEACHER", dbTeacher.getPassword());
        dbTeacher.setToken(token);
        // 写回数据库
        teacherMapper.updateById(dbTeacher);
        return dbTeacher;
    }
    //添加教师
    public int addTeacher(Teacher teacher) {

        return teacherMapper.insert(teacher);
    }
    // 删除教师
    public int deleteTeacher(Integer id) {
        courseMapper.deleteTeacherCourses(id);
        return teacherMapper.delete(id);
    }

    //修改教师
    public int updateTeacher(Teacher teacher) {
       return teacherMapper.updateById(teacher);

    }
    // 查询教师
    public List<Teacher> selectAll(Teacher teacher) {
        return teacherMapper.selectAll(teacher);
    }

}
