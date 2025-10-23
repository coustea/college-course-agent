-- ================================================
-- ⚙️ 删除表（按外键依赖顺序）
-- ================================================
DROP TABLE IF EXISTS document_progress;
DROP TABLE IF EXISTS video_progress;
DROP TABLE IF EXISTS course_documents;
DROP TABLE IF EXISTS course_videos;
DROP TABLE IF EXISTS learning_progress;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS ai_exam_answers;
DROP TABLE IF EXISTS ai_exam_attempts;
DROP TABLE IF EXISTS ai_exam_questions;
DROP TABLE IF EXISTS ai_exams;
DROP TABLE IF EXISTS student_member_scores;
DROP TABLE IF EXISTS student_submissions;
DROP TABLE IF EXISTS group_members;
DROP TABLE IF EXISTS student_groups;
DROP TABLE IF EXISTS teacher_assignments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;

# ================================================
# 用户表
# ================================================
CREATE TABLE users (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（学号或工号）',
password VARCHAR(255) NOT NULL COMMENT '用户密码（加密存储）',
role VARCHAR(20) NOT NULL COMMENT '用户角色（student/teacher）',
token VARCHAR(255) COMMENT 'JWT令牌'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ================================================
-- 学生表
-- ================================================
CREATE TABLE students (
id BIGINT PRIMARY KEY COMMENT '学生ID（对应 users.id）',
student_number VARCHAR(20) UNIQUE NOT NULL COMMENT '学号',
name VARCHAR(100) NOT NULL COMMENT '学生姓名',
class_name VARCHAR(100) COMMENT '班级',
email VARCHAR(100) COMMENT '邮箱地址',
phone VARCHAR(20) COMMENT '联系电话',
major VARCHAR(100) COMMENT '专业',
grade VARCHAR(100) COMMENT '年级',
enrollment_year YEAR COMMENT '入学年份',
status ENUM('IN_SCHOOL', 'OFF_CAMPUS_INTERNSHIP') DEFAULT 'IN_SCHOOL' COMMENT '学生状态(在校, 校外实习)',
group_status VARCHAR(20) DEFAULT 'pending' COMMENT '加入状态(待审核, 已加入)',
FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- ================================================
-- 教师表
-- ================================================
CREATE TABLE teachers (
id BIGINT PRIMARY KEY COMMENT '教师ID（对应 users.id）',
name VARCHAR(100) COMMENT '教师姓名',
email VARCHAR(100) COMMENT '邮箱地址',
phone VARCHAR(20) COMMENT '联系电话',
department VARCHAR(100) COMMENT '所属部门',
title VARCHAR(50) COMMENT '职称',
position VARCHAR(100) COMMENT '职务',
bio TEXT COMMENT '个人简介',
FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师信息表';

-- ================================================
--  课程表
-- ================================================
CREATE TABLE courses (
course_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
course_code VARCHAR(20) UNIQUE  NULL COMMENT '课程代码',
course_name VARCHAR(200) NOT NULL COMMENT '课程名称',
description TEXT COMMENT '课程描述',
credits INT DEFAULT 0 COMMENT '学分',
teacher_id BIGINT COMMENT '授课教师ID',
start_date DATE COMMENT '开课日期',
end_date DATE COMMENT '结课日期',
semester VARCHAR(20) COMMENT '学期',
max_students INT DEFAULT 100 COMMENT '最大选课人数',
resource_url VARCHAR(500) COMMENT '教学资源URL',
vindex INT DEFAULT 1 COMMENT '排序索引',
FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程信息表';

-- ================================================
--  选课表
-- ================================================
CREATE TABLE enrollments (
enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选课记录ID',
student_id BIGINT NOT NULL COMMENT '学生ID',
course_id BIGINT NOT NULL COMMENT '课程ID',
enrollment_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
status VARCHAR(20) DEFAULT 'active' COMMENT '选课状态(active, dropped, completed)',
FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
UNIQUE KEY unique_enrollment (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生选课记录表';

-- ================================================
--  学习进度表
-- ================================================
CREATE TABLE learning_progress (
progress_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学习进度ID',
student_id BIGINT NOT NULL COMMENT '学生ID',
course_id BIGINT NOT NULL COMMENT '课程ID',
completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
completion_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT '完成百分比',
time_spent INT DEFAULT 0 COMMENT '累计学习时间（秒）',
last_study_time DATETIME COMMENT '最后一次学习时间',
FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
UNIQUE KEY uniq_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习进度汇总表';

-- ================================================
--  课程视频表
-- ================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程视频资源表';

-- ================================================
--  课程文档表
-- ================================================
CREATE TABLE course_documents (
document_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
course_id BIGINT NOT NULL COMMENT '课程ID',
document_index INT NOT NULL COMMENT '文档序号',
document_title VARCHAR(255) COMMENT '文档标题',
document_url VARCHAR(500) COMMENT '文档URL地址',
upload_date DATETIME COMMENT '上传日期',
FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
UNIQUE KEY unique_document (course_id, document_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程文档资源表';

-- ================================================
--  视频进度表
-- ================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程视频学习进度表';

-- ================================================
--  文档进度表
-- ================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程文档学习进度表';

-- ================================================
--  学生分组表
-- ================================================
CREATE TABLE student_groups (
group_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分组ID',
group_name VARCHAR(100) NOT NULL COMMENT '小组名称',
class_name VARCHAR(100) COMMENT '班级名称',
group_leader_id BIGINT COMMENT '组长ID',
group_description TEXT COMMENT '小组描述',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
status VARCHAR(20) DEFAULT 'active' COMMENT '小组状态(active, disbanded)',
approval_status VARCHAR(20) DEFAULT 'pending' COMMENT '审核状态(pending, approval, rejected)',
FOREIGN KEY (group_leader_id) REFERENCES students(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生小组表';

-- ================================================
--  小组成员表
-- ================================================
CREATE TABLE group_members (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员记录ID',
group_id BIGINT NOT NULL COMMENT '小组ID',
student_id BIGINT NOT NULL COMMENT '学生ID',
student_name VARCHAR(100) COMMENT '学生姓名',
class_name VARCHAR(100) COMMENT '班级名称',
join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
role VARCHAR(20) DEFAULT 'member' COMMENT '角色(leader/member)',
join_status VARCHAR(20) DEFAULT 'pending' COMMENT '入组状态(pending, approval, rejected)',
FOREIGN KEY (group_id) REFERENCES student_groups(group_id) ON DELETE CASCADE,
FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
UNIQUE KEY uniq_group_student (group_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组成员表';

-- ================================================
--  教师发布作业表
-- ================================================
CREATE TABLE teacher_assignments (
assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业ID',
teacher_id BIGINT NOT NULL COMMENT '发布教师ID',
teacher_name VARCHAR(100) COMMENT '教师姓名',
assignment_name VARCHAR(200) NOT NULL COMMENT '作业名称',
description TEXT COMMENT '作业描述',
requirements TEXT COMMENT '作业要求',
due_date DATETIME COMMENT '截止日期',
attachment_files JSON COMMENT '作业附件（老师上传的文件JSON）',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师发布作业表';

# ================================================
#  学生小组作业提交表
# ================================================
CREATE TABLE student_submissions (
submission_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
assignment_id BIGINT NOT NULL COMMENT '作业ID',
group_id BIGINT COMMENT '分组ID',
submitted_by BIGINT NOT NULL COMMENT '提交人ID（通常为组长）',
class_name VARCHAR(100) COMMENT '班级名称',
submission_content TEXT COMMENT '提交说明/描述',
submission_files JSON COMMENT '学生上传的作品文件（JSON）',
group_comment VARCHAR(500) COMMENT '教师对整个小组作业的评语（最多500字）',
submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
status VARCHAR(20) DEFAULT 'submitted' COMMENT '提交状态(submitted, graded, returned, resubmitted)',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
FOREIGN KEY (assignment_id) REFERENCES teacher_assignments(assignment_id) ON DELETE CASCADE,
FOREIGN KEY (group_id) REFERENCES student_groups(group_id) ON DELETE CASCADE,
FOREIGN KEY (submitted_by) REFERENCES students(id) ON DELETE CASCADE,
INDEX idx_assignment_group (assignment_id, group_id),
INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生小组提交表';

-- ================================================
--  学生成员个人得分表
-- ================================================
CREATE TABLE student_member_scores (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
submission_id BIGINT NOT NULL COMMENT '对应的小组提交ID',
student_id BIGINT NOT NULL COMMENT '被评分的学生ID',
teacher_name VARCHAR(100) COMMENT '评分教师姓名',
score INT DEFAULT 0 COMMENT '个人得分',
level VARCHAR(20) COMMENT '等级（优秀、中等、合格、不及格）',
feedback TEXT COMMENT '教师对该学生的评价',
graded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
FOREIGN KEY (submission_id) REFERENCES student_submissions(submission_id) ON DELETE CASCADE,
FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
UNIQUE KEY uniq_submission_student (submission_id, student_id),
INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生个人评分表（教师姓名直接存）';

-- ================================================
--  AI 试卷/题目/作答 表
-- ================================================
CREATE TABLE ai_exams (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '试卷ID',
course_id BIGINT NOT NULL COMMENT '课程ID',
course_name VARCHAR(255) COMMENT '课程名',
student_id BIGINT NULL COMMENT '生成面向的学生(可为空)',
topic VARCHAR(255) COMMENT '主题（一般用课程名）',
question_count INT DEFAULT 0 COMMENT '题目数量',
total_score INT DEFAULT 0 COMMENT '最近一次提交的得分',
status VARCHAR(32) DEFAULT 'generated' COMMENT '状态 generated/submitted',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
INDEX idx_course (course_id),
INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成试卷';

CREATE TABLE ai_exam_questions (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
exam_id BIGINT NOT NULL COMMENT '所属试卷',
type VARCHAR(32) COMMENT 'CHOICE/JUDGE',
content TEXT COMMENT '题干',
options JSON NULL COMMENT '选项(JSON数组，判断题可为空)',
answer VARCHAR(64) COMMENT '正确答案（A/B/C/D 或 true/false）',
analysis TEXT NULL COMMENT '解析',
FOREIGN KEY (exam_id) REFERENCES ai_exams(id) ON DELETE CASCADE,
INDEX idx_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 试卷题目';

CREATE TABLE ai_exam_attempts (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交尝试ID',
exam_id BIGINT NOT NULL COMMENT '试卷ID',
student_id BIGINT NOT NULL COMMENT '学生ID',
score INT DEFAULT 0 COMMENT 'AI 评卷得分',
result_json JSON NULL COMMENT '评卷详情（可选）',
submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
FOREIGN KEY (exam_id) REFERENCES ai_exams(id) ON DELETE CASCADE,
FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
INDEX idx_exam_student (exam_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 试卷提交记录';

CREATE TABLE ai_exam_answers (
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作答ID',
attempt_id BIGINT NOT NULL COMMENT '提交尝试ID',
question_id BIGINT NOT NULL COMMENT '题目ID',
student_answer VARCHAR(255) COMMENT '学生答案',
correct BOOLEAN DEFAULT FALSE COMMENT '是否正确',
FOREIGN KEY (attempt_id) REFERENCES ai_exam_attempts(id) ON DELETE CASCADE,
FOREIGN KEY (question_id) REFERENCES ai_exam_questions(id) ON DELETE CASCADE,
INDEX idx_attempt (attempt_id),
INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 试卷作答';
