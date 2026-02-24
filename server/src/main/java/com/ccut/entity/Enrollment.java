package com.ccut.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
@Data
// 有参构造函数
@AllArgsConstructor
@NoArgsConstructor
public class Enrollment {
    private Long enrollmentId;  // 选课记录ID
    private Course course;
    private Student student;
    private Timestamp enrollmentDate;// 选课时间
    private Status status;// 选课状态
    private String enrollmentSource; // 选课来源：teacher(教师强制/必修) / student(学生自选/选修)

    public enum Status {
        active, dropped, completed
    }

    public enum Source {
        TEACHER, STUDENT
    }
}

