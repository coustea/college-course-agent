package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.entity.Enrollment;
import com.ccut.entity.Student;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.EnrollmentMapper;
import com.ccut.mapper.StudentMapper;
import com.ccut.service.EnrollmentService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 选课服务实现类
 */
@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    @Transactional
    public Enrollment enroll(Long studentId, Long courseId) {
        log.debug("执行方法：enroll, 参数：studentId={}, courseId={}", studentId, courseId);
        long startTime = System.currentTimeMillis();
        try {
            // 检查课程是否存在且已发布
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!"published".equals(course.getPublishStatus())) {
                log.error("课程未发布，无法选课：courseId={}, publishStatus={}", courseId, course.getPublishStatus());
                throw new IllegalArgumentException("该课程尚未发布，无法选课");
            }

            // 检查学生是否已选过该课程
            if (isEnrolled(studentId, courseId)) {
                log.warn("重复选课：studentId={}, courseId={}", studentId, courseId);
                throw new IllegalArgumentException("您已选过该课程，请勿重复选课");
            }

            // 执行选课（学生自选，来源为'student'）
            enrollmentMapper.upsert(studentId, courseId, "student");
            log.info("学生选课成功：studentId={}, courseId={}", studentId, courseId);

            // 构造返回的选课记录
            Enrollment enrollment = new Enrollment();
            enrollment.setCourse(course);
            enrollment.setStudent(new Student()); // 这里可以填充完整的学生信息
            enrollment.getStudent().setId(studentId);
            enrollment.setStatus(Enrollment.Status.active);
            enrollment.setEnrollmentSource("student");
            enrollment.setEnrollmentDate(new java.sql.Timestamp(System.currentTimeMillis()));

            long duration = System.currentTimeMillis() - startTime;
            log.info("选课完成：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
            log.debug("方法返回：result=选课成功");
            return enrollment;
        } catch (Exception e) {
            log.error("选课失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean drop(Long studentId, Long courseId) {
        log.debug("执行方法：drop, 参数：studentId={}, courseId={}", studentId, courseId);
        long startTime = System.currentTimeMillis();
        try {
            int rows = enrollmentMapper.drop(studentId, courseId);
            long duration = System.currentTimeMillis() - startTime;
            if (rows > 0) {
                log.info("退课成功：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
                log.debug("方法返回：result=true");
                return true;
            }
            log.warn("退课失败：studentId={}, courseId={}, 耗时={}ms", studentId, courseId, duration);
            log.debug("方法返回：result=false");
            return false;
        } catch (Exception e) {
            log.error("退课失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public int batchEnroll(Long courseId, Long teacherId, List<Long> studentIds) {
        log.debug("执行方法：batchEnroll, 参数：courseId={}, teacherId={}, studentIds count={}", 
                courseId, teacherId, studentIds != null ? studentIds.size() : 0);
        long startTime = System.currentTimeMillis();
        try {
            // 验证课程是否属于该教师
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!course.getTeacherId().equals(teacherId)) {
                log.error("无权为此课程添加学生：courseId={}, teacherId={}, courseTeacherId={}", 
                        courseId, teacherId, course.getTeacherId());
                throw new IllegalArgumentException("您无权为此课程添加学生");
            }

            // 批量添加学生
            if (studentIds == null || studentIds.isEmpty()) {
                log.warn("学生列表为空，无法批量选课");
                return 0;
            }

            int result = enrollmentMapper.batchInsert(courseId, studentIds);
            long duration = System.currentTimeMillis() - startTime;
            log.info("批量选课成功：courseId={}, studentCount={}, 耗时={}ms", courseId, studentIds.size(), duration);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("批量选课失败：courseId={}, teacherId={}, error={}", courseId, teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        log.debug("执行方法：getEnrollmentsByStudent, 参数：studentId={}", studentId);
        try {
            // 使用新方法获取包含选课来源的完整选课信息
            List<Enrollment> result = enrollmentMapper.findEnrollmentsWithCoursesByStudentId(studentId);
            log.info("查询学生选课记录成功：studentId={}, count={}", studentId, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("查询学生选课记录失败：studentId={}, error={}", studentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        log.debug("执行方法：getEnrollmentsByCourse, 参数：courseId={}", courseId);
        try {
            List<Student> students = enrollmentMapper.findStudentsByCourseId(courseId);
            List<Enrollment> enrollments = new ArrayList<>();

            Course course = courseMapper.selectById(courseId);
            if (course != null) {
                for (Student student : students) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setCourse(course);
                    enrollment.setStudent(student);
                    enrollment.setStatus(Enrollment.Status.active);
                    enrollments.add(enrollment);
                }
            }

            log.info("查询课程选课记录成功：courseId={}, count={}", courseId, enrollments.size());
            log.debug("方法返回：result count={}", enrollments.size());
            return enrollments;
        } catch (Exception e) {
            log.error("查询课程选课记录失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isEnrolled(Long studentId, Long courseId) {
        log.debug("执行方法：isEnrolled, 参数：studentId={}, courseId={}", studentId, courseId);
        try {
            int count = enrollmentMapper.countByStudentAndCourse(studentId, courseId);
            boolean result = count > 0;
            log.debug("检查选课状态：studentId={}, courseId={}, isEnrolled={}", studentId, courseId, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("检查选课状态失败：studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> importStudentsFromExcel(Long courseId, Long teacherId, MultipartFile file) {
        log.debug("执行方法：importStudentsFromExcel, 参数：courseId={}, teacherId={}, fileName={}",
                courseId, teacherId, file != null ? file.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        List<String> notFoundNumbers = new ArrayList<>();
        List<String> alreadyEnrolledNumbers = new ArrayList<>();
        List<Map<String, Object>> successStudents = new ArrayList<>();

        try {
            // 验证课程是否属于该教师
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!course.getTeacherId().equals(teacherId)) {
                log.error("无权为此课程添加学生：courseId={}, teacherId={}, courseTeacherId={}",
                        courseId, teacherId, course.getTeacherId());
                throw new IllegalArgumentException("您无权为此课程添加学生");
            }

            // 验证文件
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("请上传Excel文件");
            }
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                throw new IllegalArgumentException("请上传Excel文件（.xlsx或.xls格式）");
            }

            // 解析Excel获取学号列表
            List<String> studentNumbers = parseStudentNumbersFromExcel(file.getInputStream());
            if (studentNumbers.isEmpty()) {
                throw new IllegalArgumentException("Excel文件中未找到学号数据");
            }

            log.info("从Excel解析到 {} 个学号", studentNumbers.size());

            // 批量查询学生
            List<Student> students = studentMapper.findByStudentNumbers(studentNumbers);
            Map<String, Student> studentMap = students.stream()
                    .collect(Collectors.toMap(Student::getStudentNumber, s -> s, (a, b) -> a));

            // 找出未找到的学号
            for (String number : studentNumbers) {
                if (!studentMap.containsKey(number)) {
                    notFoundNumbers.add(number);
                }
            }

            // 检查已选课的学生
            List<Long> studentIdsToEnroll = new ArrayList<>();
            for (Student student : students) {
                if (isEnrolled(student.getId(), courseId)) {
                    alreadyEnrolledNumbers.add(student.getStudentNumber());
                } else {
                    studentIdsToEnroll.add(student.getId());
                }
            }

            // 批量添加
            int successCount = 0;
            if (!studentIdsToEnroll.isEmpty()) {
                successCount = enrollmentMapper.batchInsert(courseId, studentIdsToEnroll);
                // 构建成功学生信息
                for (Long studentId : studentIdsToEnroll) {
                    for (Student s : students) {
                        if (s.getId().equals(studentId)) {
                            Map<String, Object> info = new HashMap<>();
                            info.put("studentId", s.getId());
                            info.put("studentNumber", s.getStudentNumber());
                            info.put("name", s.getName());
                            successStudents.add(info);
                            break;
                        }
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", successCount);
            result.put("failed", alreadyEnrolledNumbers.size());
            result.put("notFound", notFoundNumbers);
            result.put("alreadyEnrolled", alreadyEnrolledNumbers);
            result.put("successStudents", successStudents);
            result.put("totalInFile", studentNumbers.size());
            result.put("message", String.format("导入完成：成功%d人，已选课%d人，未找到学号%d人",
                    successCount, alreadyEnrolledNumbers.size(), notFoundNumbers.size()));

            log.info("Excel导入学生完成：courseId={}, 成功={}, 已选课={}, 未找到={}, 耗时={}ms",
                    courseId, successCount, alreadyEnrolledNumbers.size(), notFoundNumbers.size(), duration);

            return result;
        } catch (IOException e) {
            log.error("读取Excel文件失败：{}", e.getMessage(), e);
            throw new RuntimeException("读取Excel文件失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("Excel导入学生失败：courseId={}, teacherId={}, error={}", courseId, teacherId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 从Excel文件解析学号列表
     * 支持第一列为学号或第一行有"学号"标题的列
     */
    private List<String> parseStudentNumbersFromExcel(InputStream inputStream) throws IOException {
        List<String> studentNumbers = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return studentNumbers;
            }

            // 检查第一行是否有"学号"标题
            Row headerRow = sheet.getRow(0);
            int studentNumberCol = 0; // 默认第一列

            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    String value = getCellValueAsString(cell);
                    if (value != null && value.contains("学号")) {
                        studentNumberCol = cell.getColumnIndex();
                        break;
                    }
                }
            }

            // 从第二行开始读取（跳过标题行）
            int startRow = 1;
            // 如果第一行看起来像数据（纯数字），则从第一行开始
            if (headerRow != null) {
                Cell firstCell = headerRow.getCell(studentNumberCol);
                String firstValue = getCellValueAsString(firstCell);
                if (firstValue != null && firstValue.matches("\\d+")) {
                    startRow = 0;
                }
            }

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(studentNumberCol);
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    studentNumbers.add(value.trim());
                }
            }
        }

        return studentNumbers;
    }

    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 处理学号可能以数字形式存储的情况
                long num = (long) cell.getNumericCellValue();
                return String.valueOf(num);
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }
}
