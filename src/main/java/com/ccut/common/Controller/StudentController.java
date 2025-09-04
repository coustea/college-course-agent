package com.ccut.common.Controller;

import com.ccut.common.entity.ApiResult;
import com.ccut.common.entity.Student;
import com.ccut.common.service.StudentService;
import com.ccut.common.Utils.ApiResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {
    @Autowired
    private StudentService studentService;


    @GetMapping
    public ApiResult<List<Student>> getAllStudent(@ModelAttribute Student student) {
        try {
            if (student == null) {
                return ApiResultHandler.success("查询成功", studentService.selectAll(null));
            }
            return ApiResultHandler.success("查询成功",studentService.selectAll(student));
        }catch (Exception e){
            return ApiResultHandler.buildApiResult(500,"查询失败",null);
        }
    }

    @PutMapping("/{id}")
    public ApiResult update(@RequestBody Student student, @PathVariable Integer id) {
        try {
            if (student.getId() == null) {
                student.setId(id);
            } else if (!student.getId().equals(id)) {
                return ApiResultHandler.buildApiResult(400, "路径ID与请求体ID不一致", null);
            }
            int res = studentService.updateStudent(student);
            if (res == 1){
                return ApiResultHandler.buildApiResult(200,"更新成功",student);
            }
            else {
                return ApiResultHandler.buildApiResult(500,"更新失败",null);
            }
        } catch (Exception e) {
            return ApiResultHandler.buildApiResult(500,"更新失败",null);
        }

    }


    @DeleteMapping("/{id}")
    public ApiResult delete(@PathVariable Integer id) {
        try {
            int res = studentService.deleteStudent(id);
            if (res == 1){
                return ApiResultHandler.success("删除成功",null);
            }
            else {
                return ApiResultHandler.buildApiResult(500, "删除失败", null);
            }
        } catch (Exception e) {
            return ApiResultHandler.buildApiResult(500, "删除失败", null);
        }
    }


    @PostMapping
    public ApiResult<String> add(@RequestBody Student student){
        try {
            int affectedRows = studentService.addStudent(student);
            if (affectedRows == 1){
                return ApiResultHandler.success("添加成功",student);
            }
            else{
                return ApiResultHandler.buildApiResult(500, "插入失败", null);
            }
        } catch (Exception e) {
            return ApiResultHandler.buildApiResult(500, "插入失败", null);
        }
    }


}