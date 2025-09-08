-- 用户表
-- 1. 删除学习进度表
DROP TABLE IF EXISTS learning_progress;

-- 2. 删除选课表
DROP TABLE IF EXISTS enrollments;

-- 3. 删除课程表
DROP TABLE IF EXISTS courses;

-- 4. 删除学生表
DROP TABLE IF EXISTS students;

-- 5. 删除教师表
DROP TABLE IF EXISTS teachers;

-- 6. 删除用户表
DROP TABLE IF EXISTS users;

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                       username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（学号或工号）',
                       password VARCHAR(255) NOT NULL COMMENT '用户密码（加密存储）',
                       role ENUM('student', 'teacher') NOT NULL COMMENT '用户角色（学生/教师）'
) COMMENT='用户表';

-- 学生表
CREATE TABLE students (
                          id BIGINT PRIMARY KEY COMMENT '学生ID（对应 users.id）',
                          student_number VARCHAR(20) UNIQUE NOT NULL COMMENT '学号',
                          name VARCHAR(100) NOT NULL COMMENT '学生姓名',
                          email VARCHAR(100) COMMENT '邮箱地址',
                          phone VARCHAR(20) COMMENT '联系电话',
                          major VARCHAR(100) COMMENT '专业',
                          grade VARCHAR(100) COMMENT '年级',
                          enrollment_year YEAR COMMENT '入学年份',
                          FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='学生信息表';

-- 教师表
CREATE TABLE teachers (
                          id BIGINT PRIMARY KEY COMMENT '教师ID（对应 users.id）',
                          employee_number VARCHAR(20) UNIQUE NOT NULL COMMENT '员工编号',
                          name VARCHAR(100) NOT NULL COMMENT '教师姓名',
                          email VARCHAR(100) COMMENT '邮箱地址',
                          phone VARCHAR(20) COMMENT '联系电话',
                          department VARCHAR(100) COMMENT '所属部门',
                          title VARCHAR(50) COMMENT '职称',
                          FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='教师信息表';

-- 课程表
CREATE TABLE courses (
                         course_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
                         course_code VARCHAR(20) UNIQUE NOT NULL COMMENT '课程代码',
                         course_name VARCHAR(200) NOT NULL COMMENT '课程名称',
                         description TEXT COMMENT '课程描述',
                         credits INT DEFAULT 0 COMMENT '学分',
                         teacher_id BIGINT NOT NULL COMMENT '授课教师ID',
                         start_date DATE COMMENT '开课日期',
                         end_date DATE COMMENT '结课日期',
                         semester VARCHAR(20) COMMENT '学期',
                         max_students INT DEFAULT 100 COMMENT '最大选课人数',
                         resource_url VARCHAR(500) COMMENT '教学资源URL',
                         vindex INT DEFAULT 1 COMMENT '排序索引',
                         FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT
) COMMENT='课程信息表';

-- 选课表
CREATE TABLE enrollments (
                             enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选课记录ID',
                             student_id BIGINT NOT NULL COMMENT '学生ID',
                             course_id BIGINT NOT NULL COMMENT '课程ID',
                             enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
                             status ENUM('active', 'dropped', 'completed') DEFAULT 'active' COMMENT '选课状态',
                             FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                             FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
                             UNIQUE KEY unique_enrollment (student_id, course_id)
) COMMENT='学生选课记录表';

-- 学习进度表
CREATE TABLE learning_progress (
                                   progress_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学习进度ID',
                                   student_id BIGINT NOT NULL COMMENT '学生ID',
                                   course_id BIGINT NOT NULL COMMENT '课程ID',
                                   completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
                                   completion_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT '完成百分比',
                                   time_spent INT DEFAULT 0 COMMENT '累计学习时间（秒）',
                                   FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                                   FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
                                   UNIQUE KEY unique_student_course_lesson (student_id, course_id)
) COMMENT='学习进度跟踪表';
