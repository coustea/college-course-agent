package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.Student;
import com.ccut.service.Impl.StudentServiceImpl;
import com.ccut.service.Impl.TeacherServiceImpl;
import com.ccut.mapper.EnrollmentMapper;
import com.ccut.mapper.StudentMapper;
import com.ccut.mapper.CourseMapper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    @Autowired
    private StudentServiceImpl studentService;
    @Autowired
    private TeacherServiceImpl teacherService;
    @Autowired
    private EnrollmentMapper enrollmentMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @PostMapping("/insert/students")
    public Result<String> insertStudents(@RequestBody Student student){
        try {
            // 兜底补齐 username、password、role（在 service 也有兜底，这里再提前一次）
            if (student.getUsername() == null || student.getUsername().isEmpty()) {
                student.setUsername(student.getStudentNumber());
            }
            if (student.getPassword() == null || student.getPassword().isEmpty()) {
                student.setPassword("123456");
            }
            if (student.getRole() == null) {
                // 默认 student 角色
                student.setRole(com.ccut.entity.User.Role.student);
            }

            // 由 studentService 负责先插入 users 再插入 students，保证外键正确
            int insert = studentService.insert(student);
            if(insert <= 0){
                return Result.error(500,"学生信息插入失败");
            }
            return Result.success("添加成功");
        }catch (Exception e){
            return Result.error(500,e.getMessage());
        }
    }

    @PutMapping("/update/student")
    public Result<String> updateStudentById(@RequestParam("id") Long id, @RequestBody Student student){
        try {
            if (id == null) {
                return Result.error(400, "id 不能为空");
            }
            // 以查询参数 id 为准
            student.setId(id);
            int n = studentService.update(student);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ============== 教师 CRUD（简化版） ==============
    @PostMapping("/insert/teacher")
    public Result<String> insertTeacher(@RequestBody com.ccut.entity.Teacher teacher){
        try {
            int n = teacherService.insert(teacher);
            if (n > 0) return Result.success("添加成功");
            return Result.error(500, "添加失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping("/update/teacher")
    public Result<String> updateTeacher(@RequestParam("id") Long id, @RequestBody com.ccut.entity.Teacher teacher){
        try {
            teacher.setId(id);
            int n = teacherService.update(teacher);
            if (n > 0) return Result.success("更新成功");
            return Result.error(404, "未找到或未变更");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete/teacher")
    public Result<String> deleteTeacher(@RequestParam("id") Long id){
        try {
            int n = teacherService.deleteById(id);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list/students")
    public Result<java.util.List<Student>> listStudents(){
        try {
            return Result.success(studentService.selectAll());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/list/teachers")
    public Result<java.util.List<com.ccut.entity.Teacher>> listTeachers(){
        try {
            return Result.success(teacherService.selectAll());
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 将学生加入课程（选课表 enrollments 关联）
    @PostMapping("/enroll")
    public Result<String> enroll(@RequestParam("studentId") Long studentId,
                                 @RequestParam("courseId") Long courseId){
        try {
            int n = enrollmentMapper.upsert(studentId, courseId);
            if (n > 0) return Result.success("选课成功");
            return Result.error(500, "选课失败");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 通过 Excel 按 “学号+课程代码” 批量选课（仅限已存在学生与课程）
    @PostMapping("/import/enrollments")
    public Result<String> importEnrollments(@RequestParam("file") MultipartFile file){
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            Workbook workbook;
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else if (filename.endsWith(".xls")) {
                    workbook = new HSSFWorkbook(is);
                } else {
                    return Result.error(400, "不支持的文件类型，只支持 .xls/.xlsx");
                }
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) { workbook.close(); return Result.error(400, "空工作簿"); }

            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) { workbook.close(); return Result.error(400, "空表"); }
            Row header = it.next();
            int maxCol = header.getLastCellNum();
            java.util.Map<String,Integer> idx = new java.util.HashMap<>();
            for (int i=0;i<maxCol;i++){
                String title = formatter.formatCellValue(header.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator);
                if (title==null) title="";
                title = title.replaceAll("\\s+","" ).toLowerCase();
                idx.put(title, i);
            }
            Integer snCol = idx.getOrDefault("学号", idx.get("studentnumber"));
            Integer ccCol = idx.getOrDefault("课程代码", idx.get("coursecode"));
            if (snCol == null || ccCol == null) { workbook.close(); return Result.error(400, "缺少表头：学号 或 课程代码"); }

            int success=0, skipped=0, failed=0; StringBuilder errors = new StringBuilder();
            while (it.hasNext()){
                Row row = it.next();
                String sn = formatter.formatCellValue(row.getCell(snCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator).trim();
                String code = formatter.formatCellValue(row.getCell(ccCol, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator).trim();
                if (sn.isEmpty() && code.isEmpty()) continue;
                try{
                    // 找学生与课程
                    com.ccut.entity.Student stu = studentService.getStudentByStudentNumber(sn);
                    com.ccut.entity.Course course = courseMapper.selectByCourseCode(code);
                    if (stu==null || course==null){ skipped++; continue; }
                    int n = enrollmentMapper.upsert(stu.getId(), course.getCourseId());
                    if (n>0) success++; else failed++;
                }catch (Exception ex){ failed++; if (errors.length()<1000) errors.append("行").append(row.getRowNum()+1).append(": ").append(ex.getMessage()).append("; "); }
            }
            workbook.close();
            String msg = "选课成功:"+success+" 条"+(skipped>0?", 跳过(不存在): "+skipped+" 条":"")+(failed>0?", 失败: "+failed+" 条":"");
            if (errors.length()>0) msg += "，部分错误: "+errors;
            return Result.success(msg);
        } catch (Exception e){
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/delete/student")
    public Result<String> deleteStudentById(@RequestParam("id") Long id){
        try {
            if (id == null) return Result.error(400, "id 不能为空");
            int n = studentService.deleteById(id);
            if (n > 0) return Result.success("删除成功");
            return Result.error(404, "未找到该学生");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/import/students")
    public Result<String> importStudents(@RequestParam("file") MultipartFile file){
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            Workbook workbook;
            try (InputStream is = file.getInputStream()) {
                if (filename.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(is);
                } else if (filename.endsWith(".xls")) {
                    workbook = new HSSFWorkbook(is);
                } else {
                    return Result.error(400, "不支持的文件类型，只支持 .xls/.xlsx");
                }
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                try { workbook.close(); } catch (Exception ignored) {}
                return Result.error(400, "空工作簿");
            }

            // 读取表头
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) {
                try { workbook.close(); } catch (Exception ignored) {}
                return Result.error(400, "空表");
            }
            Row header = it.next();
            Map<String, Integer> nameToIndex = new HashMap<>();
            int maxCol = header.getLastCellNum();
            for (int i = 0; i < maxCol; i++) {
                Cell c = header.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String title = formatter.formatCellValue(c, evaluator);
                if (title == null) title = "";
                title = title.replaceAll("\\s+", "").toLowerCase();
                if (!title.isEmpty()) nameToIndex.put(title, i);
            }

            // 标题别名到标准字段
            Map<String, String> alias = new HashMap<>();
            alias.put("用户名", "username"); alias.put("账号", "username"); alias.put("username", "username");
            alias.put("密码", "password"); alias.put("password", "password");
            alias.put("学号", "studentNumber"); alias.put("studentnumber", "studentNumber");
            alias.put("姓名", "name"); alias.put("name", "name");
            alias.put("邮箱", "email"); alias.put("email", "email");
            alias.put("电话", "phone"); alias.put("手机号", "phone"); alias.put("phone", "phone");
            alias.put("专业", "major"); alias.put("major", "major");
            alias.put("年级", "grade"); alias.put("grade", "grade");
            alias.put("入学年份", "enrollmentYear"); alias.put("enrollmentyear", "enrollmentYear");

            // 归一化表头索引
            Map<String, Integer> normIndex = new HashMap<>();
            for (Map.Entry<String, Integer> e : nameToIndex.entrySet()) {
                normIndex.put(e.getKey(), e.getValue());
            }
            if (normIndex.isEmpty()) {
                try { workbook.close(); } catch (Exception ignored) {}
                return Result.error(400, "未识别到任何表头，请使用提供的模板或检查表头名称");
            }

            int success = 0;
            int failed = 0;
            StringBuilder errors = new StringBuilder();
            while (it.hasNext()) {
                Row row = it.next();
                // 检查行是否全空
                boolean allBlank = true;
                for (int i = 0; i < maxCol; i++) {
                    String v = formatter.formatCellValue(row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator).trim();
                    if (!v.isEmpty()) { allBlank = false; break; }
                }
                if (allBlank) continue;

                Student s = new Student();
                // 逐字段取值
                for (Map.Entry<String,String> e : alias.entrySet()) {
                    String key = e.getKey().replaceAll("\\s+", "").toLowerCase();
                    Integer idx = normIndex.get(key);
                    if (idx == null) continue;
                    String val = formatter.formatCellValue(row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator).trim();
                    switch (e.getValue()) {
                        case "username": s.setUsername(val); break;
                        case "password": s.setPassword(val); break;
                        case "studentNumber": s.setStudentNumber(val); break;
                        case "name": s.setName(val); break;
                        case "email": s.setEmail(val); break;
                        case "phone": s.setPhone(val); break;
                        case "major": s.setMajor(val); break;
                        case "grade": s.setGrade(val); break;
                        case "enrollmentYear":
                            try {
                                if (val != null) {
                                    String digits = val.replaceAll("[^0-9]", "");
                                    if (digits.length() >= 4) digits = digits.substring(0, 4);
                                    if (!digits.isEmpty()) {
                                        int year = Integer.parseInt(digits);
                                        if (year >= 1901 && year <= 2155) {
                                            s.setEnrollmentYear(year);
                                        } else {
                                            s.setEnrollmentYear(null);
                                        }
                                    }
                                }
                            } catch (Exception ignore) { s.setEnrollmentYear(null); }
                            break;
                        default: break;
                    }
                }

                if (s.getUsername() == null || s.getUsername().isEmpty()) {
                    s.setUsername(s.getStudentNumber());
                }
                if (s.getPassword() == null || s.getPassword().isEmpty()) {
                    s.setPassword("123456");
                }

                try {
                    // 重复学号检测：存在则跳过，计入失败并给出原因
                    Student existed = studentService.getStudentByStudentNumber(s.getStudentNumber());
                    if (existed != null) {
                        failed++;
                        if (errors.length() < 1000) {
                            errors.append("行").append(row.getRowNum()+1).append(": 学号已存在; ");
                        }
                    } else {
                        int n = studentService.insert(s);
                        if (n > 0) success++; else failed++;
                    }
                } catch (Exception ex) {
                    failed++;
                    if (errors.length() < 1000) {
                        errors.append("行").append(row.getRowNum()+1).append(": ").append(ex.getMessage()).append("; ");
                    }
                }
            }

            try { workbook.close(); } catch (Exception ignored) {}
            String msg = "导入成功: " + success + " 条" + (failed>0? (", 失败: "+failed+" 条") : "");
            if (errors.length() > 0) msg += "，部分错误: " + errors;
            return Result.success(msg);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
