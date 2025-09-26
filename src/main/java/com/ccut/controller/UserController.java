package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.entity.User;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.Impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private StudentServiceImpl studentService;

    @PostMapping("/insert")
    public Result<User> insert(@RequestBody User user){
        int result = userService.insert(user);
        System.out.println(result);
        if (result > 0) {
            return Result.success(user);
        }
        return Result.error(500, "添加失败");
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody User user){
        return Result.success("登录成功");
    }


    @PostMapping("/excel")
    public Result<String> insertByExcel(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            log.error("文件为空");
            return Result.error(400, "文件为空");
        }

        List<Student> students = new ArrayList<>();
        String filename = file.getOriginalFilename().toLowerCase();


        if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
            log.error("文件格式错误");
            return Result.error(400, "文件格式错误");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = filename.endsWith(".xls")
                     ? new HSSFWorkbook(inputStream)
                     : new XSSFWorkbook(inputStream)) {

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String studentNumber = formatter.formatCellValue(row.getCell(1), evaluator).trim();
                String name = formatter.formatCellValue(row.getCell(2), evaluator).trim();
                String className = formatter.formatCellValue(row.getCell(4), evaluator).trim();

                // 过滤无效行
                if (!studentNumber.matches("\\d{6,}") || name.isEmpty() || !className.matches("\\d+")) {
                    continue;
                }

                Student student = new Student();
                student.setUsername(studentNumber);
                student.setPassword(studentNumber);
                student.setStudentNumber(studentNumber);
                student.setName(name);
                student.setClassName(className);
                students.add(student);
            }
        }

        if (students.isEmpty()) {
            return Result.error(400, "Excel中没有有效的学生数据");
        }

        int successCount = 0;

        try {
            for (Student student : students) {
                User exist = userService.getByUsername(student.getUsername());
                if (exist != null){
                    student.setId(exist.getId());
                    log.warn("用户 {} 已存在，ID为 {}", student.getUsername(), exist.getId());
                }else{
                    User user = new User(student.getUsername(), student.getPassword(), User.Role.student);
                    int res = userService.insert(user);
                    if (res <= 0) {
                        log.error("添加用户失败：{}", student);
                        continue;
                    }
                    student.setId(user.getId());
                    System.out.println(student);
                }
                Student existStudent = studentService.getStudentByStudentNumber(student.getStudentNumber());
                if (existStudent != null){
                    log.warn("学生 {} 已存在，ID为 {}", student.getStudentNumber(), existStudent.getId());
                    continue;
                }
                int res1 = studentService.insert(student);
                if (res1 <= 0) {
                    log.error("添加学生失败：{}", student);
                    continue;
                }
                successCount++;
            }
            return Result.success("成功导入 " + successCount + " 条数据");

        } catch (Exception e) {
            log.error("导入异常", e);
            return Result.error(500, e.getMessage());
        }
    }
    @DeleteMapping
    public Result<String> deleteAll(){
        int res = userService.deleteAll();
        return Result.success("删除成功");
    }
}
