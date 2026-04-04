package com.ccut.service.Impl;

import com.ccut.entity.Student;
import com.ccut.entity.User;
import com.ccut.exception.BusinessException;
import com.ccut.mapper.UserMapper;
import com.ccut.service.StudentService;
import com.ccut.service.UserService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentService studentService;


    @Override
    public int insert(User user) {
        log.debug("执行方法：insert, 参数：user={}", user != null ? user.getUsername() : "null");
        try {
            int result = userMapper.insertUser(user);
            log.info("用户插入成功：username={}, result={}", user != null ? user.getUsername() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("用户插入失败：username={}, error={}", user != null ? user.getUsername() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int updateUser(User user) {
        log.debug("执行方法：updateUser, 参数：user={}", user != null ? "id=" + user.getId() : "null");
        try {
            int result = userMapper.updateUser(user);
            log.info("用户更新成功：userId={}, result={}", user != null ? user.getId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("用户更新失败：userId={}, error={}", user != null ? user.getId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int deleteAll() {
        log.debug("执行方法：deleteAll");
        try {
            int result = userMapper.deleteAll();
            log.info("删除所有用户成功：result={}", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("删除所有用户失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public User getByUsername(String username) {
        log.debug("执行方法：getByUsername, 参数：username={}", username);
        try {
            User result = userMapper.getUserByUsername(username);
            log.debug("方法返回：result={}", result != null ? "id=" + result.getId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询用户失败：username={}, error={}", username, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String importStudentsFromExcel(MultipartFile file) {
        String filename = file.getOriginalFilename().toLowerCase();

        if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
            throw new BusinessException(400, "文件格式错误");
        }

        List<Student> students = new ArrayList<>();

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
                String statusString = formatter.formatCellValue(row.getCell(4), evaluator).trim();
                String className = formatter.formatCellValue(row.getCell(7), evaluator).trim();

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

                Student.Status studentStatus = Student.Status.IN_SCHOOL;
                if ("实习".equals(statusString)) {
                    studentStatus = Student.Status.OFF_CAMPUS_INTERNSHIP;
                }
                student.setStatus(studentStatus);

                students.add(student);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "Excel 解析失败：" + e.getMessage());
        }

        if (students.isEmpty()) {
            throw new BusinessException(400, "Excel 中没有有效的学生数据");
        }

        log.info("[批量导入] 解析完成，共获取 {} 条有效数据", students.size());
        int successCount = 0;

        for (Student student : students) {
            User exist = userMapper.getUserByUsername(student.getUsername());
            if (exist != null) {
                student.setId(exist.getId());
                log.warn("[批量导入] 用户 {} 已存在，ID 为 {}", student.getUsername(), exist.getId());
            } else {
                User user = new User(student.getUsername(), student.getPassword(), User.Role.student);
                int res = userMapper.insertUser(user);
                if (res <= 0) {
                    log.error("[批量导入] 添加用户失败：{}", student);
                    continue;
                }
                student.setId(user.getId());
            }
            Student existStudent = studentService.getStudentByStudentNumber(student.getStudentNumber());
            if (existStudent != null) {
                log.warn("[批量导入] 学生 {} 已存在，ID 为 {}", student.getStudentNumber(), existStudent.getId());
                continue;
            }
            int res1 = studentService.insert(student);
            if (res1 <= 0) {
                log.error("[批量导入] 添加学生失败：{}", student);
                continue;
            }
            successCount++;
        }

        log.info("[批量导入] 导入完成，成功导入 {} 条数据", successCount);
        return "成功导入 " + successCount + " 条数据";
    }
}
