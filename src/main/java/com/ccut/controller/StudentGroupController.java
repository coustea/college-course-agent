package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.StudentGroup;
import com.ccut.mapper.StudentGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student-group")
public class StudentGroupController {

    @Autowired
    private StudentGroupMapper studentGroupMapper;

    @PostMapping("/insert")
    public Result<String> insert(@RequestBody StudentGroup g){
        try { int n = studentGroupMapper.insert(g); return n>0? Result.success("添加成功"): Result.error(500, "添加失败"); }
        catch (Exception e){ return Result.error(500, e.getMessage()); }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody StudentGroup g){
        try { int n = studentGroupMapper.updateById(g); return n>0? Result.success("更新成功"): Result.error(404, "未找到或未变更"); }
        catch (Exception e){ return Result.error(500, e.getMessage()); }
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("id") Long id){
        try { int n = studentGroupMapper.deleteById(id); return n>0? Result.success("删除成功"): Result.error(404, "未找到"); }
        catch (Exception e){ return Result.error(500, e.getMessage()); }
    }

    @GetMapping("/detail")
    public Result<StudentGroup> detail(@RequestParam("id") Long id){
        try { return Result.success(studentGroupMapper.selectById(id)); }
        catch (Exception e){ return Result.error(500, e.getMessage()); }
    }

    @GetMapping("/list-by-course")
    public Result<java.util.List<StudentGroup>> listByCourse(@RequestParam("courseId") Long courseId){
        try { return Result.success(studentGroupMapper.listByCourseId(courseId)); }
        catch (Exception e){ return Result.error(500, e.getMessage()); }
    }

    @GetMapping("/list-by-student")
    public Result<java.util.List<StudentGroup>> listByStudent(@RequestParam("studentId") Long studentId){
        try { return Result.success(studentGroupMapper.listByStudentId(studentId)); }
        catch (Exception e){ return Result.error(500, e.getMessage()); }
    }
}


