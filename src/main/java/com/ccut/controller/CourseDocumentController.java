package com.ccut.controller;

import com.ccut.entity.CourseDocument;
import com.ccut.entity.Result;
import com.ccut.mapper.CourseDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/course/document")
public class CourseDocumentController {

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @PostMapping("/insert")
    public Result<String> insert(@RequestBody CourseDocument doc){
        try {
            int n = courseDocumentMapper.insert(doc);
            if (n>0) return Result.success("添加成功");
            return Result.error(500, "添加失败");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody CourseDocument doc){
        try {
            int n = courseDocumentMapper.updateById(doc);
            if (n>0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("documentId") Long documentId){
        try {
            int n = courseDocumentMapper.deleteById(documentId);
            if (n>0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<java.util.List<CourseDocument>> list(@RequestParam("courseId") Long courseId){
        try {
            return Result.success(courseDocumentMapper.findByCourseId(courseId));
        }catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }
}


