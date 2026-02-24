package com.ccut.controller;

import com.ccut.dto.Result;
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
    public Result<User> insert(@RequestBody User user) {
        log.debug("收到插入用户请求：URI=/api/user/insert, 参数：user={}", user);
        try {
            log.info("执行插入用户业务：username={}", user.getUsername());
            int result = userService.insert(user);
            log.debug("插入用户结果：result={}, userId={}", result, user.getId());
            if (result > 0) {
                log.info("用户插入成功：username={}", user.getUsername());
                return Result.success(user);
            }
            log.warn("用户插入失败：username={}", user.getUsername());
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            log.error("插入用户异常：user={}, 错误：{}", user, e.getMessage(), e);
            return Result.error(500, "添加失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable("id") Long id, @RequestBody User user) {
        log.debug("收到更新用户请求：URI=/api/user/{}, 参数：id={}, user={}", id, user);
        try {
            user.setId(id);
            log.info("执行更新用户业务：id={}, username={}", id, user.getUsername());
            int res = userService.updateUser(user);
            if (res <= 0) {
                log.error("更新用户失败：id={}, user={}", id, user);
                return Result.error(500, "更新用户失败");
            }
            log.debug("更新用户成功：id={}", id);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新用户异常：id={}, 错误：{}", id, e.getMessage(), e);
            return Result.error(500, "更新失败：" + e.getMessage());
        }
    }

    @PostMapping("/excel")
    public Result<String> insertByExcel(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("[批量导入] 收到导入请求：URI=/api/user/excel, 文件名：{}, 文件大小：{} bytes",
                 file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            log.error("[批量导入] 文件为空");
            return Result.error(400, "文件为空");
        }

        List<Student> students = new ArrayList<>();
        String filename = file.getOriginalFilename().toLowerCase();
        log.info("[批量导入] 文件名：{}", filename);

        if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
            log.error("[批量导入] 文件格式错误：{}", filename);
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

                // 新增：读取学生状态（假设在第 4 列，即 E 列）
                String statusString = formatter.formatCellValue(row.getCell(4), evaluator).trim();
                // 注意：将读取班级的列索引从 4 调整为 7（H 列），以匹配您提供的文件片段中的班级位置
                String className = formatter.formatCellValue(row.getCell(7), evaluator).trim();

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
                student.setGroupStatus("pending");

                // ======== 修改点：根据状态设置枚举值 ========
                Student.Status studentStatus = Student.Status.IN_SCHOOL; // 默认在校
                if ("实习".equals(statusString)) {
                    // 如果读取到的状态是"实习"，则设置为校外实习
                    studentStatus = Student.Status.OFF_CAMPUS_INTERNSHIP;
                } else if (statusString.isEmpty()) {
                    // 如果状态为空，保持默认的 IN_SCHOOL（在校），这里可以省略 else if
                }
                student.setStatus(studentStatus);
                // ===================================

                students.add(student);
            }
        }

        if (students.isEmpty()) {
            log.warn("[批量导入] Excel 中没有有效的学生数据");
            return Result.error(400, "Excel 中没有有效的学生数据");
        }

        log.info("[批量导入] 解析完成，共获取 {} 条有效数据", students.size());
        int successCount = 0;

        try {
            for (Student student : students) {
                log.debug("[批量导入] 处理学生：{}", student.getStudentNumber());
                User exist = userService.getByUsername(student.getUsername());
                if (exist != null){
                    student.setId(exist.getId());
                    log.warn("用户 {} 已存在，ID 为 {}", student.getUsername(), exist.getId());
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
                    log.warn("学生 {} 已存在，ID 为 {}", student.getStudentNumber(), existStudent.getId());
                    continue;
                }
                int res1 = studentService.insert(student);
                if (res1 <= 0) {
                    log.error("添加学生失败：{}", student);
                    continue;
                }
                successCount++;
            }
            log.info("[批量导入] 导入完成，成功导入 {} 条数据", successCount);
            return Result.success("成功导入 " + successCount + " 条数据");

        } catch (Exception e) {
            log.error("[批量导入] 导入异常", e);
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping
    public Result<String> deleteAll(){
        log.debug("收到删除所有用户请求：URI=/api/user");
        try {
            log.info("执行删除所有用户业务");
            int res = userService.deleteAll();
            log.debug("删除所有用户结果：result={}", res);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除所有用户异常：错误：{}", e.getMessage(), e);
            return Result.error(500, "删除失败：" + e.getMessage());
        }
    }
}
