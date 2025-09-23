-- 删除表（按正确顺序，避免外键约束问题）
DROP TABLE IF EXISTS document_progress;
DROP TABLE IF EXISTS video_progress;
DROP TABLE IF EXISTS course_documents;
DROP TABLE IF EXISTS course_videos;
DROP TABLE IF EXISTS learning_progress;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS group_members;
DROP TABLE IF EXISTS student_groups;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;


CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（学号或工号）',
    password VARCHAR(255) NOT NULL COMMENT '用户密码（加密存储）',
    role ENUM('student', 'teacher') NOT NULL COMMENT '用户角色（学生/教师/管理员）'
) COMMENT='用户表';

-- ===============================
-- 学生表（扩展用户信息）
-- ===============================
CREATE TABLE students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID（对应 users.id）',
  student_number VARCHAR(20) UNIQUE NOT NULL COMMENT '学号',
  name VARCHAR(100) NOT NULL COMMENT '学生姓名',
  email VARCHAR(100) COMMENT '邮箱地址',
  phone VARCHAR(20) COMMENT '联系电话',
  major VARCHAR(100) COMMENT '专业',
  grade VARCHAR(100) COMMENT '年级',
  enrollment_year YEAR COMMENT '入学年份',
  FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='学生信息表';

-- ===============================
-- 教师表（扩展用户信息）
-- ===============================
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

-- ===============================
-- 课程表
-- ===============================
CREATE TABLE courses (
 course_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
 course_code VARCHAR(20) UNIQUE NOT NULL COMMENT '课程代码',
 course_name VARCHAR(200) NOT NULL COMMENT '课程名称',
 description TEXT COMMENT '课程描述',
 credits INT DEFAULT 0 COMMENT '学分',
 teacher_id BIGINT NULL COMMENT '授课教师ID',
 start_date DATE COMMENT '开课日期',
 end_date DATE COMMENT '结课日期',
 semester VARCHAR(20) COMMENT '学期',
 max_students INT DEFAULT 100 COMMENT '最大选课人数',
 resource_url VARCHAR(500) COMMENT '教学资源URL',
 vindex INT DEFAULT 1 COMMENT '排序索引',
 FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) COMMENT='课程信息表';

-- ===============================
-- 选课表
-- ===============================
CREATE TABLE enrollments (
     enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选课记录ID',
     student_id BIGINT NOT NULL COMMENT '学生ID',
     course_id BIGINT NOT NULL COMMENT '课程ID',
     enrollment_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
     status ENUM('active', 'dropped', 'completed') DEFAULT 'active' COMMENT '选课状态',
     FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
     FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
     UNIQUE KEY unique_enrollment (student_id, course_id)
) COMMENT='学生选课记录表';

-- ===============================
-- 学习进度表（按课程维度，可做汇总）
-- ===============================
CREATE TABLE learning_progress (
           progress_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学习进度ID',
           student_id BIGINT NOT NULL COMMENT '学生ID',
           course_id BIGINT NOT NULL COMMENT '课程ID',
           completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
           completion_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT '完成百分比',
           time_spent INT DEFAULT 0 COMMENT '累计学习时间（秒）',
           last_study_time DATETIME DEFAULT NULL COMMENT '最后一次学习时间',
           FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
           FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
           UNIQUE KEY unique_student_course (student_id, course_id)
) COMMENT='学习进度汇总表';

-- ===============================
-- 课程视频表
-- ===============================
CREATE TABLE course_videos (
       video_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '视频ID',
       course_id BIGINT NOT NULL COMMENT '课程ID',
       video_index INT NOT NULL COMMENT '视频集数（顺序编号）',
       video_title VARCHAR(255) COMMENT '视频标题',
       video_url VARCHAR(500) COMMENT '视频URL地址',
       duration INT DEFAULT 0 COMMENT '视频时长（秒）',
       upload_date DATETIME COMMENT '上传日期',
       FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
       UNIQUE KEY unique_video (course_id, video_index)
) COMMENT='课程视频资源表';

-- ===============================
-- 课程文档表
-- ===============================
CREATE TABLE course_documents (
          document_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
          course_id BIGINT NOT NULL COMMENT '课程ID',
          document_index INT NOT NULL COMMENT '文档序号（顺序编号）',
          document_title VARCHAR(255) COMMENT '文档标题',
          document_url VARCHAR(500) COMMENT '文档URL地址（可指向PDF、Word等）',
          upload_date DATETIME COMMENT '上传日期',
          FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
          UNIQUE KEY unique_document (course_id, document_index)
) COMMENT='课程文档资源表';

-- ===============================
-- 视频进度表（按视频维度）
-- ===============================
CREATE TABLE video_progress (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        student_id BIGINT NOT NULL,
        course_id BIGINT NOT NULL,
        video_id BIGINT NOT NULL,
        watched_seconds INT DEFAULT 0 COMMENT '已观看时长（秒）',
        completed BOOLEAN DEFAULT FALSE COMMENT '是否完成该视频',
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
        FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
        FOREIGN KEY (video_id) REFERENCES course_videos(video_id) ON DELETE CASCADE,
        UNIQUE KEY uniq_student_video (student_id, video_id)
) COMMENT='课程视频学习进度表';

-- ===============================
-- 文档进度表（按文档维度）
-- ===============================
CREATE TABLE document_progress (
           id BIGINT PRIMARY KEY AUTO_INCREMENT,
           student_id BIGINT NOT NULL,
           course_id BIGINT NOT NULL,
           document_id BIGINT NOT NULL,
           time_spent INT DEFAULT 0 COMMENT '阅读时间（秒）',
           max_scroll_pct DECIMAL(5,2) DEFAULT 0.00 COMMENT '最大阅读进度（百分比）',
           completed BOOLEAN DEFAULT FALSE COMMENT '是否完成该文档',
           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
           FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
           FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
           FOREIGN KEY (document_id) REFERENCES course_documents(document_id) ON DELETE CASCADE,
           UNIQUE KEY uniq_student_document (student_id, document_id)
) COMMENT='课程文档学习进度表';

DROP TABLE IF EXISTS group_members;
DROP TABLE IF EXISTS student_groups;
-- ===============================
-- 学生分组表（小组信息）
-- ===============================
CREATE TABLE student_groups (
        group_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分组ID',
        course_id BIGINT NOT NULL COMMENT '课程ID',
        group_name VARCHAR(100) NOT NULL COMMENT '小组名称',
        group_leader_id BIGINT  NULL COMMENT '组长ID',
        teacher_id BIGINT COMMENT '审核教师ID',
        group_description TEXT COMMENT '小组描述',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        status ENUM('active','disbanded') DEFAULT 'active' COMMENT '小组状态',
        approval_status ENUM('pending','approved','rejected') DEFAULT 'pending' COMMENT '审核状态',
        FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
        FOREIGN KEY (group_leader_id) REFERENCES students(id) ON DELETE SET NULL,
        FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
        UNIQUE KEY uniq_leader_course (course_id, group_leader_id) COMMENT '同一课程一个学生只能创建一个队伍'
) COMMENT='学生小组表';

-- ===============================
-- 分组成员表（小组成员关系）
-- ===============================
CREATE TABLE group_members (
       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员记录ID',
       group_id BIGINT NOT NULL COMMENT '小组ID',
       course_id BIGINT NOT NULL COMMENT '课程ID',
       student_id BIGINT NOT NULL COMMENT '学生ID',
       student_name VARCHAR(100) COMMENT '学生姓名',
       join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
       role ENUM('leader','member') DEFAULT 'member' COMMENT '角色',
       join_status ENUM('pending','approved','rejected') DEFAULT 'pending' COMMENT '入组状态',
       FOREIGN KEY (group_id) REFERENCES student_groups(group_id) ON DELETE CASCADE,
       FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
       FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
       UNIQUE KEY uniq_group_student (group_id, student_id),
       UNIQUE KEY uniq_course_student (course_id, student_id) COMMENT '同一课程学生只能加入一个队伍'
) COMMENT='小组成员表';