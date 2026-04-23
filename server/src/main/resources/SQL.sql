
-- ================================================
-- CCUT 高校教学管理系统 — 完整数据库脚本
-- ================================================
-- 使用说明：
-- 1. 此脚本包含所有表结构 + 测试数据
-- 2. 直接在 MySQL 中执行即可初始化数据库
-- 3. 警告：会删除并重建所有表，请谨慎使用！
-- ================================================

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除所有表（按依赖关系逆序）
DROP TABLE IF EXISTS teaching_style_profiles;

DROP TABLE IF EXISTS value_assessments;

DROP TABLE IF EXISTS sentiment_records;

DROP TABLE IF EXISTS teaching_evaluations;

DROP TABLE IF EXISTS student_behavior_profiles;

DROP TABLE IF EXISTS learning_path_records;

DROP TABLE IF EXISTS wrong_question;

DROP TABLE IF EXISTS ai_exam_answers;

DROP TABLE IF EXISTS ai_exam_attempts;

DROP TABLE IF EXISTS ai_exam_questions;

DROP TABLE IF EXISTS ai_exams;

DROP TABLE IF EXISTS recommendation;

DROP TABLE IF EXISTS ideology_resource_recommendation;

DROP TABLE IF EXISTS ideology_resource_tag_rel;

DROP TABLE IF EXISTS ideology_resource_tags;

DROP TABLE IF EXISTS ideology_resources;

DROP TABLE IF EXISTS messages;

DROP TABLE IF EXISTS conversations;

DROP TABLE IF EXISTS student_member_scores;

DROP TABLE IF EXISTS student_submissions;

DROP TABLE IF EXISTS teacher_assignments;

DROP TABLE IF EXISTS group_members;

DROP TABLE IF EXISTS student_groups;

DROP TABLE IF EXISTS weekly_study_time;

DROP TABLE IF EXISTS video_progress;

DROP TABLE IF EXISTS document_progress;

DROP TABLE IF EXISTS learning_progress;

DROP TABLE IF EXISTS enrollments;

DROP TABLE IF EXISTS chapters;

DROP TABLE IF EXISTS course_videos;

DROP TABLE IF EXISTS course_documents;

DROP TABLE IF EXISTS courses;

DROP TABLE IF EXISTS teachers;

DROP TABLE IF EXISTS students;

DROP TABLE IF EXISTS users;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ================================================
-- 1. 用户表
-- ================================================
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（学号或工号）',
    password VARCHAR(255) NOT NULL COMMENT '用户密码（加密存储）',
    role VARCHAR(20) NOT NULL COMMENT '用户角色（student/teacher）',
    token VARCHAR(255) COMMENT 'JWT令牌'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ================================================
-- 2. 学生表
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
    status ENUM(
        'IN_SCHOOL',
        'OFF_CAMPUS_INTERNSHIP'
    ) DEFAULT 'IN_SCHOOL' COMMENT '学生状态(在校, 校外实习)',
    group_status VARCHAR(20) DEFAULT 'pending' COMMENT '加入状态(待审核, 已加入)',
    FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生信息表';

-- ================================================
-- 3. 教师表
-- ================================================
CREATE TABLE teachers (
    id BIGINT PRIMARY KEY COMMENT '教师ID（对应 users.id）',
    employee_number VARCHAR(50) COMMENT '工号',
    name VARCHAR(100) COMMENT '教师姓名',
    email VARCHAR(100) COMMENT '邮箱地址',
    phone VARCHAR(20) COMMENT '联系电话',
    department VARCHAR(100) COMMENT '所属部门',
    title VARCHAR(50) COMMENT '职称',
    position VARCHAR(100) COMMENT '职务',
    bio TEXT COMMENT '个人简介',
    FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师信息表';

-- ================================================
-- 4. 课程表
-- ================================================
CREATE TABLE courses (
    course_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_code VARCHAR(20) UNIQUE NULL COMMENT '课程代码',
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
    publish_status VARCHAR(20) DEFAULT 'draft' COMMENT '发布状态(draft/published)',
    published_at DATETIME COMMENT '发布时间',
    FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程信息表';

-- ================================================
-- 5. 选课表
-- ================================================
CREATE TABLE enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选课记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    enrollment_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    status VARCHAR(20) DEFAULT 'active' COMMENT '选课状态(active, dropped, completed)',
    enrollment_source VARCHAR(20) DEFAULT 'student' COMMENT '选课来源：teacher(教师强制添加/必修) / student(学生自选/选修)',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (student_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生选课记录表';

-- ================================================
-- 6. 学习进度表
-- ================================================
CREATE TABLE learning_progress (
    progress_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学习进度ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    completion_percentage DECIMAL(5, 2) DEFAULT 0.00 COMMENT '完成百分比',
    time_spent INT DEFAULT 0 COMMENT '累计学习时间（秒）',
    last_study_time DATETIME COMMENT '最后一次学习时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    UNIQUE KEY uniq_student_course (student_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学习进度汇总表';

-- ================================================
-- 7. 章节表（支持树形结构：章 -> 节）
-- ================================================
CREATE TABLE chapters (
    chapter_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '章节ID',
    course_id BIGINT NOT NULL COMMENT '所属课程ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父章节ID（NULL表示一级章，非NULL表示小节）',
    title VARCHAR(255) NOT NULL COMMENT '章节标题',
    chapter_index INT DEFAULT 0 COMMENT '章节序号（同级排序）',
    chapter_type VARCHAR(20) DEFAULT 'chapter' COMMENT '类型：chapter(章) / section(节)',
    content_type TINYINT DEFAULT 0 COMMENT '内容类型：0(无内容) 1(视频) 2(文档)',
    sort INT DEFAULT 0 COMMENT '排序权重（数字越大越靠前）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_course_parent (course_id, parent_id),
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES chapters (chapter_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程章节表';

-- ================================================
-- 8. 课程视频表
-- ================================================
CREATE TABLE course_videos (
    video_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '视频ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    chapter_id BIGINT DEFAULT NULL COMMENT '所属章节ID（小节ID）',
    video_index INT NOT NULL COMMENT '视频集数（顺序编号）',
    video_title VARCHAR(255) COMMENT '视频标题',
    video_url VARCHAR(500) COMMENT '视频URL地址',
    duration INT DEFAULT 0 COMMENT '视频时长（秒）',
    upload_date DATETIME COMMENT '上传日期',
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    UNIQUE KEY unique_video (course_id, video_index),
    INDEX idx_chapter_id (chapter_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程视频资源表';

-- ================================================
-- 9. 课程文档表
-- ================================================
CREATE TABLE course_documents (
    document_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    chapter_id BIGINT DEFAULT NULL COMMENT '所属章节ID（小节ID）',
    document_index INT NOT NULL COMMENT '文档序号',
    document_title VARCHAR(255) COMMENT '文档标题',
    document_url VARCHAR(500) COMMENT '文档URL地址',
    upload_date DATETIME COMMENT '上传日期',
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    UNIQUE KEY unique_document (course_id, document_index),
    INDEX idx_chapter_id (chapter_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程文档资源表';

-- ================================================
-- 9.5 思政资源库表
-- ================================================
CREATE TABLE ideology_resources (
    resource_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '思政资源ID',
    course_id BIGINT NULL COMMENT '关联课程ID',
    chapter_id BIGINT NULL COMMENT '关联章节ID',
    video_id BIGINT NULL COMMENT '关联视频ID',
    document_id BIGINT NULL COMMENT '关联文档ID',
    title VARCHAR(255) NOT NULL COMMENT '资源标题',
    resource_type VARCHAR(30) DEFAULT 'document' COMMENT '资源类型(case/policy/video/document/activity)',
    content_summary TEXT COMMENT '内容摘要',
    source_url VARCHAR(500) COMMENT '资源地址',
    value_theme VARCHAR(100) COMMENT '价值主题',
    applicable_scene VARCHAR(200) COMMENT '适用教学场景',
    keywords VARCHAR(500) COMMENT '关键词，逗号分隔',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度(easy/medium/hard)',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态(draft/published)',
    created_by BIGINT NULL COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE SET NULL,
    FOREIGN KEY (chapter_id) REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    FOREIGN KEY (video_id) REFERENCES course_videos (video_id) ON DELETE SET NULL,
    FOREIGN KEY (document_id) REFERENCES course_documents (document_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_course_id (course_id),
    INDEX idx_status (status),
    INDEX idx_value_theme (value_theme),
    FULLTEXT KEY ft_title_summary_keywords (title, content_summary, keywords)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程思政资源库表';

CREATE TABLE ideology_resource_tags (
    tag_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称',
    tag_type VARCHAR(30) DEFAULT 'keyword' COMMENT '标签类型(theme/keyword/scene/value)',
    description VARCHAR(500) COMMENT '标签说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_tag_name (tag_name),
    INDEX idx_tag_type (tag_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '思政资源标签表';

CREATE TABLE ideology_resource_tag_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    resource_id BIGINT NOT NULL COMMENT '思政资源ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (resource_id) REFERENCES ideology_resources (resource_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES ideology_resource_tags (tag_id) ON DELETE CASCADE,
    UNIQUE KEY uk_resource_tag (resource_id, tag_id),
    INDEX idx_tag_id (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '思政资源标签关联表';

CREATE TABLE ideology_resource_recommendation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    resource_id BIGINT NOT NULL COMMENT '思政资源ID',
    course_id BIGINT NULL COMMENT '课程ID',
    reason VARCHAR(500) COMMENT '推荐理由',
    score DOUBLE NOT NULL COMMENT '推荐分数',
    recommendation_type VARCHAR(50) NOT NULL COMMENT '推荐类型(TAG_MATCH/PROGRESS_SCENE/POPULAR)',
    has_clicked BOOLEAN DEFAULT FALSE COMMENT '是否已点击',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    clicked_at DATETIME NULL COMMENT '点击时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES ideology_resources (resource_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE SET NULL,
    INDEX idx_student_id (student_id),
    INDEX idx_resource_id (resource_id),
    INDEX idx_course_id (course_id),
    INDEX idx_student_score (student_id, score)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '思政资源推荐表';

-- ================================================
-- 10. 视频进度表
-- ================================================
CREATE TABLE video_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    watched_seconds INT DEFAULT 0 COMMENT '已观看时长（秒）',
    completed BOOLEAN DEFAULT FALSE COMMENT '是否完成该视频',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES course_videos (video_id) ON DELETE CASCADE,
    UNIQUE KEY uniq_student_video (student_id, video_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程视频学习进度表';

-- ================================================
-- 11. 文档进度表
-- ================================================
CREATE TABLE document_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    time_spent INT DEFAULT 0 COMMENT '阅读时间（秒）',
    max_scroll_pct DECIMAL(5, 2) DEFAULT 0.00 COMMENT '最大阅读进度（百分比）',
    completed BOOLEAN DEFAULT FALSE COMMENT '是否完成该文档',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES course_documents (document_id) ON DELETE CASCADE,
    UNIQUE KEY uniq_student_document (student_id, document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程文档学习进度表';

-- ================================================
-- 12. 每周学习时间统计表
-- ================================================
CREATE TABLE weekly_study_time (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT DEFAULT NULL COMMENT '课程ID（NULL表示总学习时间）',
    week_start_date DATE NOT NULL COMMENT '本周开始日期（周一）',
    total_seconds INT DEFAULT 0 COMMENT '本周累计学习时间（秒）',
    last_study_time DATETIME COMMENT '本周最后一次学习时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_course_week (
        student_id,
        course_id,
        week_start_date
    ),
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_week_start_date (week_start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每周学习时间统计表';

-- ================================================
-- 13. 学生分组表
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
    FOREIGN KEY (group_leader_id) REFERENCES students (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生小组表';

-- ================================================
-- 14. 小组成员表
-- ================================================
CREATE TABLE group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员记录ID',
    group_id BIGINT NOT NULL COMMENT '小组ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    student_name VARCHAR(100) COMMENT '学生姓名',
    student_number VARCHAR(20) COMMENT '学号',
    class_name VARCHAR(100) COMMENT '班级名称',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    role VARCHAR(20) DEFAULT 'member' COMMENT '角色(leader/member)',
    join_status VARCHAR(20) DEFAULT 'pending' COMMENT '入组状态(pending, approval, rejected)',
    FOREIGN KEY (group_id) REFERENCES student_groups (group_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    UNIQUE KEY uniq_group_student (group_id, student_id),
    INDEX idx_student_number (student_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小组成员表';

-- ================================================
-- 15. 教师发布作业表
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
    FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师发布作业表';

-- ================================================
-- 16. 学生小组作业提交表
-- ================================================
CREATE TABLE student_submissions (
    submission_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
    assignment_id BIGINT NOT NULL COMMENT '作业ID',
    group_id BIGINT COMMENT '分组ID',
    submitted_by BIGINT NOT NULL COMMENT '提交人ID（通常为组长）',
    class_name VARCHAR(100) COMMENT '班级名称',
    submission_content TEXT COMMENT '提交说明/描述',
    submission_files JSON COMMENT '学生上传的作品文件（JSON）',
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    status VARCHAR(20) DEFAULT 'submitted' COMMENT '提交状态(submitted, graded, returned, resubmitted)',
    group_comment VARCHAR(500) COMMENT '教师对整个小组作业的评语（最多500字）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (assignment_id) REFERENCES teacher_assignments (assignment_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES student_groups (group_id) ON DELETE CASCADE,
    FOREIGN KEY (submitted_by) REFERENCES students (id) ON DELETE CASCADE,
    INDEX idx_assignment_group (assignment_id, group_id),
    INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生小组提交表';

-- ================================================
-- 17. 学生成员个人得分表
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
    FOREIGN KEY (submission_id) REFERENCES student_submissions (submission_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    UNIQUE KEY uniq_submission_student (submission_id, student_id),
    INDEX idx_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生个人评分表';

-- ================================================
-- 18. AI 生成试卷表
-- ================================================
CREATE TABLE ai_exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '试卷ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    course_name VARCHAR(255) COMMENT '课程名',
    student_id BIGINT NULL COMMENT '生成面向的学生(可为空)',
    topic VARCHAR(255) COMMENT '主题',
    question_count INT DEFAULT 0 COMMENT '题目数量',
    total_score INT DEFAULT 0 COMMENT '最近一次提交的得分',
    status VARCHAR(32) DEFAULT 'generated' COMMENT '状态 generated/submitted',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    video_id BIGINT NULL COMMENT '关联的视频ID（可为空）',
    document_id BIGINT NULL COMMENT '关联的文档ID（可为空）',
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT fk_aiexam_video FOREIGN KEY (video_id) REFERENCES course_videos (video_id) ON DELETE SET NULL,
    CONSTRAINT fk_aiexam_document FOREIGN KEY (document_id) REFERENCES course_documents (document_id) ON DELETE SET NULL,
    INDEX idx_course (course_id),
    INDEX idx_student (student_id),
    INDEX idx_video (video_id),
    INDEX idx_document (document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 生成试卷';

-- ================================================
-- 19. AI 试卷题目表
-- ================================================
CREATE TABLE ai_exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    exam_id BIGINT NOT NULL COMMENT '所属试卷',
    type VARCHAR(32) COMMENT 'CHOICE/JUDGE',
    content TEXT COMMENT '题干',
    options JSON NULL COMMENT '选项(JSON数组，判断题可为空)',
    answer VARCHAR(64) COMMENT '正确答案（A/B/C/D 或 true/false）',
    analysis TEXT NULL COMMENT '解析',
    FOREIGN KEY (exam_id) REFERENCES ai_exams (id) ON DELETE CASCADE,
    INDEX idx_exam (exam_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 试卷题目';

-- ================================================
-- 20. AI 试卷提交记录表
-- ================================================
CREATE TABLE ai_exam_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交尝试ID',
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    score INT DEFAULT 0 COMMENT 'AI 评卷得分',
    result_json JSON NULL COMMENT '评卷详情（可选）',
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    FOREIGN KEY (exam_id) REFERENCES ai_exams (id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    INDEX idx_exam_student (exam_id, student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 试卷提交记录';

-- ================================================
-- 21. AI 试卷作答表
-- ================================================
CREATE TABLE ai_exam_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作答ID',
    attempt_id BIGINT NOT NULL COMMENT '提交尝试ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    student_answer VARCHAR(255) COMMENT '学生答案',
    correct BOOLEAN DEFAULT FALSE COMMENT '是否正确',
    FOREIGN KEY (attempt_id) REFERENCES ai_exam_attempts (id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES ai_exam_questions (id) ON DELETE CASCADE,
    INDEX idx_attempt (attempt_id),
    INDEX idx_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 试卷作答';

-- ================================================
-- 22.5 课程推荐表
-- ================================================
CREATE TABLE recommendation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    reason VARCHAR(500) COMMENT '推荐理由',
    score DOUBLE NOT NULL COMMENT '推荐分数（0-1）',
    recommendation_type VARCHAR(50) NOT NULL COMMENT '推荐类型：CONTENT_BASED, COLLABORATIVE, POPULAR',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    has_clicked BOOLEAN DEFAULT FALSE COMMENT '是否已点击',
    clicked_at DATETIME COMMENT '点击时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_created_at (created_at),
    INDEX idx_student_score (student_id, score)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程推荐表';

-- ================================================
-- 23. 教学评价表
-- ================================================
CREATE TABLE teaching_evaluations (
    evaluation_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT COMMENT '学生ID（NULL表示课程整体评价）',
    teacher_id BIGINT COMMENT '教师ID',
    content_integration_score DOUBLE DEFAULT 0 COMMENT '内容融入度评分(0-100)',
    interaction_score DOUBLE DEFAULT 0 COMMENT '师生互动评分(0-100)',
    participation_score DOUBLE DEFAULT 0 COMMENT '学生参与度评分(0-100)',
    value_recognition_score DOUBLE DEFAULT 0 COMMENT '价值认同评分(0-100)',
    total_score DOUBLE DEFAULT 0 COMMENT '综合评分(0-100)',
    evaluation_period VARCHAR(20) DEFAULT 'weekly' COMMENT '评价周期(weekly/monthly/semester)',
    evaluation_type VARCHAR(20) DEFAULT 'student' COMMENT '评价类型(course/student/teacher)',
    remark TEXT COMMENT '评价备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE SET NULL,
    INDEX idx_course_id (course_id),
    INDEX idx_student_id (student_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_period (evaluation_period)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教学评价表';

-- ================================================
-- 24. 学生行为画像表
-- ================================================
CREATE TABLE student_behavior_profiles (
    profile_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '画像ID',
    student_id BIGINT UNIQUE NOT NULL COMMENT '学生ID',
    preferred_study_time VARCHAR(20) COMMENT '偏好学习时段(morning/afternoon/evening/night)',
    morning_ratio DOUBLE DEFAULT 0 COMMENT '上午学习占比',
    afternoon_ratio DOUBLE DEFAULT 0 COMMENT '下午学习占比',
    evening_ratio DOUBLE DEFAULT 0 COMMENT '晚上学习占比',
    preferred_resource_type VARCHAR(30) COMMENT '偏好资源类型(video/document/interactive)',
    video_preference DOUBLE DEFAULT 0.33 COMMENT '视频偏好度',
    document_preference DOUBLE DEFAULT 0.33 COMMENT '文档偏好度',
    interactive_preference DOUBLE DEFAULT 0.33 COMMENT '互动偏好度',
    avg_session_duration DOUBLE COMMENT '平均学习时长(分钟)',
    interaction_frequency INT DEFAULT 0 COMMENT '互动频率(次数/周)',
    completion_rate DOUBLE DEFAULT 0 COMMENT '完成率',
    consistency_score DOUBLE DEFAULT 0 COMMENT '学习连贯性评分',
    learning_style_tag VARCHAR(30) COMMENT '学习风格标签(visual/auditory/kinesthetic/mixed)',
    engagement_level VARCHAR(20) COMMENT '参与度等级(high/medium/low)',
    ideology_click_rate DOUBLE DEFAULT 0 COMMENT '思政资源点击率',
    ideology_completion_rate DOUBLE DEFAULT 0 COMMENT '思政资源完成率',
    last_analyzed_at DATETIME COMMENT '最后分析时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    INDEX idx_engagement_level (engagement_level)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生行为画像表';

-- ================================================
-- 25. 学习路径记录表
-- ================================================
CREATE TABLE learning_path_records (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT COMMENT '课程ID',
    resource_type VARCHAR(30) COMMENT '资源类型(video/document/ideology/exam)',
    resource_id BIGINT COMMENT '资源ID',
    resource_title VARCHAR(255) COMMENT '资源标题',
    action_type VARCHAR(30) COMMENT '行为类型(view/start/complete/interact/review)',
    duration_seconds INT DEFAULT 0 COMMENT '持续时间(秒)',
    progress_percent DOUBLE DEFAULT 0 COMMENT '进度百分比',
    previous_resource_id BIGINT COMMENT '前一个资源ID',
    device_type VARCHAR(20) COMMENT '设备类型(pc/mobile/tablet)',
    session_id VARCHAR(100) COMMENT '会话ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学习路径记录表';

-- ================================================
-- 27. 情感分析记录表
-- ================================================
CREATE TABLE sentiment_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT COMMENT '课程ID',
    sentiment_type VARCHAR(20) NOT NULL COMMENT '情感类型：positive/neutral/negative',
    positive_score DOUBLE DEFAULT 0 COMMENT '积极情感得分 (0-1)',
    negative_score DOUBLE DEFAULT 0 COMMENT '消极情感得分 (0-1)',
    neutral_score DOUBLE DEFAULT 0 COMMENT '中性情感得分 (0-1)',
    source_type VARCHAR(30) COMMENT '来源类型：chat/reflection/discussion',
    source_content TEXT COMMENT '来源内容（文本摘要）',
    source_id BIGINT COMMENT '来源ID',
    ideology_related BOOLEAN DEFAULT FALSE COMMENT '是否与思政相关',
    ideology_theme VARCHAR(100) COMMENT '思政主题标签',
    analyzed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_sentiment_type (sentiment_type),
    INDEX idx_analyzed_at (analyzed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '情感分析记录表';

-- ================================================
-- 28. 价值认同评估表
-- ================================================
CREATE TABLE value_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评估ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    patriotism_score DOUBLE DEFAULT 0 COMMENT '爱国主义认同 (0-100)',
    social_responsibility_score DOUBLE DEFAULT 0 COMMENT '社会责任认同 (0-100)',
    professional_ethics_score DOUBLE DEFAULT 0 COMMENT '职业道德认同 (0-100)',
    innovation_score DOUBLE DEFAULT 0 COMMENT '创新精神认同 (0-100)',
    cultural_confidence_score DOUBLE DEFAULT 0 COMMENT '文化自信认同 (0-100)',
    total_score DOUBLE DEFAULT 0 COMMENT '总体认同度 (0-100)',
    assessment_level VARCHAR(20) COMMENT '评估等级：high/medium/low',
    previous_score DOUBLE COMMENT '上期得分',
    trend_direction VARCHAR(20) COMMENT '趋势方向：up/stable/down',
    trend_value DOUBLE COMMENT '变化幅度',
    assessment_basis TEXT COMMENT '评估依据说明',
    assessment_period VARCHAR(20) DEFAULT 'monthly' COMMENT '评估周期：weekly/monthly',
    assessed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_assessed_at (assessed_at),
    INDEX idx_student_course (student_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价值认同评估表';

-- ================================================
-- 29. 教师授课风格画像表
-- ================================================
CREATE TABLE teaching_style_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '画像ID',
    teacher_id BIGINT UNIQUE NOT NULL COMMENT '教师ID',
    interaction_level DOUBLE DEFAULT 0 COMMENT '互动程度 (0-100)',
    content_depth DOUBLE DEFAULT 0 COMMENT '内容深度 (0-100)',
    practicality DOUBLE DEFAULT 0 COMMENT '实用性 (0-100)',
    innovation DOUBLE DEFAULT 0 COMMENT '创新性 (0-100)',
    ideology_integration DOUBLE DEFAULT 0 COMMENT '思政融入度 (0-100)',
    ideology_approach VARCHAR(30) COMMENT '思政融入方式：implicit/explicit/mixed',
    main_value_themes VARCHAR(500) COMMENT '主要价值主题（逗号分隔）',
    ideology_resource_count INT DEFAULT 0 COMMENT '思政资源使用数量',
    avg_student_satisfaction DOUBLE DEFAULT 0 COMMENT '平均学生满意度',
    avg_engagement_rate DOUBLE DEFAULT 0 COMMENT '平均参与率',
    style_tag VARCHAR(30) COMMENT '风格标签：interactive/practical/academic/innovative',
    last_analyzed_at DATETIME COMMENT '最后分析时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教师授课风格画像表';

-- ================================================
-- 26. AI 会话表
-- ================================================
CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    conversation_id VARCHAR(255) UNIQUE NOT NULL COMMENT '会话唯一标识（username:序号）',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    sequence_num INT NOT NULL COMMENT '该用户的第几个会话',
    title VARCHAR(255) DEFAULT 'AI助手对话' COMMENT '会话标题',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_conversation_id (conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI对话会话表';

-- ================================================
-- 23. AI 聊天消息表
-- ================================================
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id VARCHAR(255) NOT NULL COMMENT '会话唯一标识（关联conversations表）',
    username VARCHAR(50) NOT NULL COMMENT '用户名（方便直接查询）',
    role VARCHAR(20) NOT NULL COMMENT '角色（user/assistant/system）',
    content TEXT NOT NULL COMMENT '消息内容',
    files TEXT NULL COMMENT '附件信息（JSON字符串，存储文件列表）',
    sequence_num INT NOT NULL COMMENT '消息序号（同一会话内递增）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tokens_used INT DEFAULT 0 COMMENT '使用的token数（可选）',
    FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI聊天消息表';

-- ================================================
-- 24. 错题本表
-- ================================================
CREATE TABLE wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    question_id BIGINT NOT NULL COMMENT '题目ID（关联 ai_exam_questions）',
    exam_id BIGINT NOT NULL COMMENT '考试ID（关联 ai_exams）',
    course_id BIGINT NOT NULL COMMENT '课程ID（冗余字段，方便查询）',
    wrong_answer VARCHAR(500) NULL COMMENT '学生的错误答案',
    correct_answer VARCHAR(500) NULL COMMENT '正确答案（冗余字段）',
    note TEXT NULL COMMENT '错题笔记/注释',
    wrong_count INT NOT NULL DEFAULT 1 COMMENT '错误次数（每次答错累加）',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '连续答对次数（答对累加，答错归零）',
    is_mastered TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已掌握（0-未掌握，1-已掌握）',
    first_wrong_time DATETIME NOT NULL COMMENT '首次答错时间',
    last_wrong_time DATETIME NOT NULL COMMENT '最后答错时间',
    mastered_time DATETIME NULL COMMENT '掌握时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_student_id (student_id),
    KEY idx_question_id (question_id),
    KEY idx_exam_id (exam_id),
    KEY idx_course_id (course_id),
    KEY idx_correct_count (correct_count),
    KEY idx_student_course (student_id, course_id),
    UNIQUE KEY uk_student_question (student_id, question_id),
    FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES ai_exam_questions (id) ON DELETE CASCADE,
    FOREIGN KEY (exam_id) REFERENCES ai_exams (id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '错题本表';

-- ================================================
-- 更多测试数据
-- ================================================

-- 默认账号（项目说明中使用的基础账号）
INSERT INTO users (username, password, role) VALUES
('student', '123456', 'student'),
('teacher', '123456', 'teacher');

INSERT INTO students (id, student_number, name, class_name, email, phone, major, grade, enrollment_year, status, group_status) VALUES
((SELECT id FROM users WHERE username = 'student'), '2024001', '王小明', '计算机24-1班', 'wangxiaoming@student.ccut.edu.cn', '13812345678', '计算机科学与技术', '2024级', 2024, 'IN_SCHOOL', 'pending');

INSERT INTO teachers (id, employee_number, name, email, phone, department, title, position, bio) VALUES
((SELECT id FROM users WHERE username = 'teacher'), 'T2024001', '张教授', 'zhang@teacher.ccut.edu.cn', '13987654321', '计算机学院', '教授', '课程负责人', '主要研究方向：计算机系统结构、教学智能化。');

-- 更多学生账号
INSERT INTO users (username, password, role) VALUES
('student2', '123456', 'student'),
('student3', '123456', 'student'),
('student4', '123456', 'student'),
('student5', '123456', 'student'),
('student6', '123456', 'student');

INSERT INTO students (id, student_number, name, class_name, email, phone, major, grade, enrollment_year, status, group_status) VALUES
((SELECT id FROM users WHERE username = 'student2'), '2024002', '李华', '计算机24-1班', 'lihua@student.ccut.edu.cn', '13812345679', '计算机科学与技术', '2024级', 2024, 'IN_SCHOOL', 'pending'),
((SELECT id FROM users WHERE username = 'student3'), '2024003', '张伟', '计算机24-2班', 'zhangwei@student.ccut.edu.cn', '13812345680', '计算机科学与技术', '2024级', 2024, 'IN_SCHOOL', 'pending'),
((SELECT id FROM users WHERE username = 'student4'), '2024004', '王芳', '软件工程24-1班', 'wangfang@student.ccut.edu.cn', '13812345681', '软件工程', '2024级', 2024, 'IN_SCHOOL', 'pending'),
((SELECT id FROM users WHERE username = 'student5'), '2024005', '刘洋', '软件工程24-1班', 'liuyang@student.ccut.edu.cn', '13812345682', '软件工程', '2024级', 2024, 'IN_SCHOOL', 'pending'),
((SELECT id FROM users WHERE username = 'student6'), '2024006', '陈静', '数据科学24-1班', 'chenjing@student.ccut.edu.cn', '13812345683', '数据科学', '2024级', 2024, 'IN_SCHOOL', 'pending');

-- 更多教师账号
INSERT INTO users (username, password, role) VALUES
('teacher2', '123456', 'teacher'),
('teacher3', '123456', 'teacher');

INSERT INTO teachers (id, employee_number, name, email, phone, department, title, position, bio) VALUES
((SELECT id FROM users WHERE username = 'teacher2'), 'T2024002', '李副教授', 'li@teacher.ccut.edu.cn', '13987654322', '软件学院', '副教授', '教研室主任', '主要研究方向：软件工程、分布式系统。'),
((SELECT id FROM users WHERE username = 'teacher3'), 'T2024003', '王讲师', 'wang@teacher.ccut.edu.cn', '13987654323', '人工智能学院', '讲师', NULL, '主要研究方向：机器学习、深度学习。');

-- 课程数据
INSERT INTO courses (course_code, course_name, description, credits, teacher_id, start_date, end_date, semester, max_students, publish_status, published_at) VALUES
('CS101', '计算机科学导论', '计算机科学入门课程，涵盖计算机基础、编程基础、数据结构入门等内容。', 3, (SELECT id FROM users WHERE username = 'teacher'), '2024-09-01', '2025-01-15', '2024秋季', 100, 'published', NOW()),
('CS201', '数据结构与算法', '深入学习常用数据结构和算法设计与分析方法。', 4, (SELECT id FROM users WHERE username = 'teacher'), '2024-09-01', '2025-01-15', '2024秋季', 80, 'published', NOW()),
('SE101', '软件工程基础', '软件工程的基本概念、原理和方法，包括需求分析、设计、测试等。', 3, (SELECT id FROM users WHERE username = 'teacher2'), '2024-09-01', '2025-01-15', '2024秋季', 60, 'published', NOW()),
('AI101', '人工智能导论', '人工智能基础理论、机器学习基础、神经网络入门。', 3, (SELECT id FROM users WHERE username = 'teacher3'), '2024-09-01', '2025-01-15', '2024秋季', 50, 'published', NOW()),
('CS301', '操作系统原理', '操作系统基本概念、进程管理、内存管理、文件系统等。', 4, (SELECT id FROM users WHERE username = 'teacher'), '2025-02-15', '2025-06-30', '2025春季', 60, 'draft', NULL);

-- 章节数据（为CS101课程创建章节结构）
-- 先插入一级章节
INSERT INTO chapters (course_id, parent_id, title, chapter_index, chapter_type, content_type, sort) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, '第一章 计算机概述', 1, 'chapter', 0, 100),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, '第二章 编程基础', 2, 'chapter', 0, 90);

-- 再插入小节（使用变量存储父章节ID）
SET @chapter1_id = (SELECT chapter_id FROM chapters WHERE title = '第一章 计算机概述' AND course_id = (SELECT course_id FROM courses WHERE course_code = 'CS101') LIMIT 1);
SET @chapter2_id = (SELECT chapter_id FROM chapters WHERE title = '第二章 编程基础' AND course_id = (SELECT course_id FROM courses WHERE course_code = 'CS101') LIMIT 1);

INSERT INTO chapters (course_id, parent_id, title, chapter_index, chapter_type, content_type, sort) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), @chapter1_id, '1.1 计算机发展历史', 1, 'section', 1, 99),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), @chapter1_id, '1.2 计算机系统组成', 2, 'section', 2, 98),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), @chapter2_id, '2.1 程序设计基础', 1, 'section', 1, 89),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), @chapter2_id, '2.2 数据类型与变量', 2, 'section', 1, 88);

-- 视频资源（为CS101课程）
INSERT INTO course_videos (course_id, chapter_id, video_index, video_title, video_url, duration, upload_date) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, 1, '计算机发展历史概述', '/media/videos/cs101/01.mp4', 1800, NOW()),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, 2, '冯·诺依曼体系结构', '/media/videos/cs101/02.mp4', 2100, NOW()),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, 3, '编程语言发展史', '/media/videos/cs101/03.mp4', 2400, NOW()),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), NULL, 1, '数据结构概述', '/media/videos/cs201/01.mp4', 1500, NOW()),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), NULL, 2, '数组与链表', '/media/videos/cs201/02.mp4', 2700, NOW());

-- 文档资源（为CS101课程）
INSERT INTO course_documents (course_id, chapter_id, document_index, document_title, document_url, upload_date) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, 1, '计算机科学导论课件-第一章', '/media/docs/cs101/chapter1.pdf', NOW()),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), NULL, 2, '编程基础实验手册', '/media/docs/cs101/lab.pdf', NOW()),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), NULL, 1, '数据结构与算法讲义', '/media/docs/cs201/notes.pdf', NOW());

-- 选课记录
INSERT INTO enrollments (student_id, course_id, enrollment_date, status, enrollment_source) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'SE101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student4'), (SELECT course_id FROM courses WHERE course_code = 'SE101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student5'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student6'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), NOW(), 'active', 'student'),
((SELECT id FROM users WHERE username = 'student6'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), NOW(), 'active', 'student');

-- 学习进度（部分学生已有学习记录）
INSERT INTO learning_progress (student_id, course_id, completion_percentage, time_spent, last_study_time) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 35.50, 7200, NOW()),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), 15.00, 3600, NOW()),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 20.00, 4500, NOW()),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 50.00, 9000, NOW());

-- 视频学习进度
INSERT INTO video_progress (student_id, course_id, video_id, watched_seconds, completed) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 1, 1800, TRUE),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 2, 1500, FALSE),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 1, 1200, FALSE);

-- 学生小组
INSERT INTO student_groups (group_name, class_name, group_leader_id, group_description, status, approval_status) VALUES
('人工智能研究小组', '计算机24-1班', (SELECT id FROM users WHERE username = 'student'), '致力于人工智能技术的学习与实践', 'active', 'approval'),
('Web开发小队', '软件工程24-1班', (SELECT id FROM users WHERE username = 'student4'), '前后端开发技术学习小组', 'active', 'approval');

-- 小组成员
INSERT INTO group_members (group_id, student_id, student_name, student_number, class_name, role, join_status) VALUES
((SELECT group_id FROM student_groups WHERE group_name = '人工智能研究小组'), (SELECT id FROM users WHERE username = 'student'), '王小明', '2024001', '计算机24-1班', 'leader', 'approval'),
((SELECT group_id FROM student_groups WHERE group_name = '人工智能研究小组'), (SELECT id FROM users WHERE username = 'student2'), '李华', '2024002', '计算机24-1班', 'member', 'approval'),
((SELECT group_id FROM student_groups WHERE group_name = '人工智能研究小组'), (SELECT id FROM users WHERE username = 'student3'), '张伟', '2024003', '计算机24-2班', 'member', 'approval'),
((SELECT group_id FROM student_groups WHERE group_name = 'Web开发小队'), (SELECT id FROM users WHERE username = 'student4'), '王芳', '2024004', '软件工程24-1班', 'leader', 'approval'),
((SELECT group_id FROM student_groups WHERE group_name = 'Web开发小队'), (SELECT id FROM users WHERE username = 'student5'), '刘洋', '2024005', '软件工程24-1班', 'member', 'approval');

-- 教师作业
INSERT INTO teacher_assignments (teacher_id, teacher_name, assignment_name, description, requirements, due_date) VALUES
((SELECT id FROM users WHERE username = 'teacher'), '张教授', '数据结构课程设计', '实现一个简单的学生信息管理系统', '1. 使用链表或数组存储学生信息\n2. 实现增删改查功能\n3. 编写测试用例', DATE_ADD(NOW(), INTERVAL 14 DAY)),
((SELECT id FROM users WHERE username = 'teacher2'), '李副教授', '软件需求分析报告', '为一个外卖APP编写需求分析报告', '1. 使用UML用例图描述功能需求\n2. 编写至少5个用户故事\n3. 绘制界面原型图', DATE_ADD(NOW(), INTERVAL 21 DAY));

-- AI会话示例
INSERT INTO conversations (conversation_id, username, sequence_num, title) VALUES
('student:1', 'student', 1, '关于数据结构的学习讨论'),
('student:2', 'student', 2, 'Python编程问题'),
('teacher:1', 'teacher', 1, '课程设计讨论');

-- AI消息示例
INSERT INTO messages (conversation_id, username, role, content, sequence_num) VALUES
('student:1', 'student', 'user', '请解释一下什么是时间复杂度？', 1),
('student:1', 'student', 'assistant', '时间复杂度是衡量算法运行时间随输入规模增长而增长的度量。它描述了算法执行所需时间与输入数据规模之间的关系。\n\n常见的时间复杂度从低到高有：\n- O(1)：常数时间\n- O(log n)：对数时间\n- O(n)：线性时间\n- O(n log n)：线性对数时间\n- O(n²)：平方时间\n\n理解时间复杂度对于选择合适的算法非常重要！', 2);

-- ================================================
-- 思政资源库测试数据
-- ================================================

-- 思政资源标签
INSERT INTO ideology_resource_tags (tag_name, tag_type, description) VALUES
('爱国主义', 'theme', '弘扬爱国主义精神，培养家国情怀'),
('社会责任', 'theme', '培养社会责任感和公民意识'),
('职业道德', 'theme', '职业操守与职业道德教育'),
('创新精神', 'theme', '培养创新意识和创新能力'),
('文化自信', 'theme', '传承中华优秀传统文化'),
('科学精神', 'keyword', '求真务实、勇于探索'),
('工匠精神', 'keyword', '精益求精、追求卓越'),
('团队协作', 'keyword', '团结合作、共同进步'),
('课堂导入', 'scene', '适合课程开头导入'),
('案例分析', 'scene', '适合案例分析教学'),
('实践应用', 'scene', '适合实践教学环节'),
('课后拓展', 'scene', '适合课后自主学习');

-- 思政资源（100+条）
INSERT INTO ideology_resources (course_id, title, resource_type, content_summary, source_url, value_theme, applicable_scene, keywords, difficulty, status, created_by) VALUES
-- 计算机科学导论相关思政资源
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '中国计算机发展历程', 'case', '介绍中国计算机事业从无到有、从弱到强的发展历程，展现老一辈科学家的爱国情怀和奋斗精神。包括银河计算机、天河超级计算机等重大成就。', '/ideology/cs_history.pdf', '爱国主义', '课堂导入', '计算机历史,爱国情怀,科技强国', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '图灵奖华人得主介绍', 'document', '介绍姚期智等华人图灵奖得主的学术贡献和科学精神，激励学生追求卓越。', '/ideology/turing_chinese.pdf', '创新精神', '案例分析', '图灵奖,科学精神,学术追求', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '开源精神与协作文化', 'document', '介绍开源软件运动的发展，阐述开源精神中的共享、协作、共赢理念，培养学生的团队协作意识。', '/ideology/opensource.pdf', '团队协作', '案例分析', '开源,协作,共享', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '中国芯的崛起之路', 'case', '讲述中国芯片产业从被封锁到自主研发的艰辛历程，培养学生的爱国情怀和创新意识。', '/ideology/china_chip.pdf', '爱国主义', '课堂导入', '芯片,自主创新,科技自立', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),

-- 数据结构与算法相关思政资源
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '算法与公平正义', 'document', '探讨算法在社会治理中的应用，分析算法偏见问题，培养学生的社会责任感和伦理意识。', '/ideology/algorithm_ethics.pdf', '社会责任', '案例分析', '算法伦理,公平正义,社会责任', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '数据安全与隐私保护', 'document', '介绍数据安全法律法规，分析大数据时代的隐私保护问题，培养学生的法律意识和职业道德。', '/ideology/data_privacy.pdf', '职业道德', '案例分析', '数据安全,隐私保护,职业道德', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '中国数学家的故事', 'case', '介绍华罗庚、陈景润等中国数学家的奋斗故事，弘扬科学精神和爱国情怀。', '/ideology/chinese_math.pdf', '爱国主义', '课堂导入', '数学家,科学精神,爱国', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '算法优化与工匠精神', 'document', '通过算法优化的案例，阐述精益求精的工匠精神，培养学生追求卓越的品质。', '/ideology/algorithm_craftsman.pdf', '创新精神', '案例分析', '算法优化,工匠精神,追求卓越', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),

-- 软件工程相关思政资源
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '软件工程师的职业操守', 'document', '介绍软件工程师职业道德规范，包括知识产权保护、用户隐私尊重、代码质量责任等内容。', '/ideology/software_ethics.pdf', '职业道德', '课堂导入', '职业操守,道德规范,责任意识', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '开源项目中的团队协作', 'case', '以Linux内核开发为例，介绍全球协作的开源模式，培养学生的团队协作精神和沟通能力。', '/ideology/linux_collaboration.pdf', '团队协作', '案例分析', '开源协作,团队合作,沟通能力', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '中国软件产业发展', 'document', '介绍中国软件产业从跟随到引领的发展历程，增强学生的民族自信心和行业认同感。', '/ideology/china_software.pdf', '文化自信', '课堂导入', '软件产业,行业发展,民族自信', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '代码审查与责任意识', 'document', '阐述代码审查的重要性，培养学生的责任意识和严谨态度。', '/ideology/code_review.pdf', '职业道德', '案例分析', '代码审查,责任意识,质量意识', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),

-- 人工智能相关思政资源
((SELECT course_id FROM courses WHERE course_code = 'AI101'), 'AI伦理与社会责任', 'document', '探讨人工智能发展中的伦理问题，包括算法公平性、AI武器化、就业影响等，培养学生的社会责任意识。', '/ideology/ai_ethics.pdf', '社会责任', '案例分析', 'AI伦理,社会责任,科技向善', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '中国人工智能发展战略', 'document', '介绍中国在人工智能领域的战略布局和重大成就，培养学生的爱国情怀和使命感。', '/ideology/china_ai.pdf', '爱国主义', '课堂导入', '人工智能,国家战略,科技强国', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '深度学习与创新精神', 'case', '介绍深度学习的发展历程，从神经网络到深度学习的创新突破，培养学生的创新意识。', '/ideology/deep_learning_innovation.pdf', '创新精神', '案例分析', '深度学习,创新突破,科学探索', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), 'AI赋能传统文化', 'case', '介绍人工智能在传统文化保护、传承中的应用案例，增强学生的文化自信。', '/ideology/ai_culture.pdf', '文化自信', '案例分析', 'AI应用,传统文化,文化保护', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher3')),

-- 通用思政资源
(NULL, '大国工匠精神解读', 'document', '解读工匠精神的内涵，结合科技行业案例，培养学生的职业追求和敬业精神。', '/ideology/craftsman_spirit.pdf', '职业道德', '课堂导入', '工匠精神,职业追求,敬业', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '科学家精神与科技强国', 'document', '介绍科学家精神的内涵，结合钱学森、邓稼先等科学家的故事，培养学生的爱国情怀和科学精神。', '/ideology/scientist_spirit.pdf', '爱国主义', '课堂导入', '科学家精神,爱国,科技强国', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '数字时代的公民素养', 'document', '介绍数字时代公民应具备的素养，包括信息辨别能力、网络安全意识、数字道德等。', '/ideology/digital_citizen.pdf', '社会责任', '案例分析', '数字素养,网络安全,公民意识', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
(NULL, '创新创业与时代责任', 'document', '介绍创新创业的社会价值，培养学生的社会责任感和创新意识。', '/ideology/innovation_responsibility.pdf', '创新精神', '案例分析', '创新创业,社会责任,时代使命', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher3'));

-- 更多思政资源（补充到100+条）
INSERT INTO ideology_resources (course_id, title, resource_type, content_summary, value_theme, applicable_scene, keywords, difficulty, status, created_by) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '计算机伦理学导论', 'document', '介绍计算机伦理学的基本概念和原则。', '职业道德', '课堂导入', '计算机伦理,道德原则', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '网络空间安全战略', 'document', '解读国家网络空间安全战略。', '爱国主义', '案例分析', '网络安全,国家安全', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '编程之美与工匠精神', 'document', '探讨代码美学与工匠精神的联系。', '创新精神', '案例分析', '编程美学,工匠精神', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '数据结构中的哲学思想', 'document', '从哲学角度解读数据结构设计。', '创新精神', '案例分析', '哲学思想,数据结构', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '算法竞赛与团队精神', 'case', '介绍ACM竞赛中的团队协作案例。', '团队协作', '案例分析', 'ACM竞赛,团队协作', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '软件测试与责任意识', 'document', '阐述软件测试中的责任担当。', '职业道德', '案例分析', '软件测试,责任意识', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '需求分析中的用户关怀', 'document', '培养以用户为中心的设计理念。', '社会责任', '案例分析', '需求分析,用户关怀', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '机器学习与社会公平', 'document', '探讨机器学习中的公平性问题。', '社会责任', '案例分析', '机器学习,公平性,社会正义', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '机器人伦理三定律', 'document', '介绍阿西莫夫机器人三定律及其现代意义。', '职业道德', '课堂导入', '机器人伦理,科技伦理', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
(NULL, '中华优秀传统文化与科技创新', 'document', '探讨传统文化与现代科技的融合。', '文化自信', '案例分析', '传统文化,科技创新', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '创新创业教育与实践', 'activity', '创新创业实践活动的组织与指导。', '创新精神', '实践应用', '创新创业,实践活动', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
(NULL, '科技论文写作规范', 'document', '介绍科技论文写作的学术规范。', '职业道德', '案例分析', '学术规范,论文写作', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher3'));

-- 资源库补充数据（视频可先保持草稿并留空 source_url）
INSERT INTO ideology_resources (course_id, title, resource_type, content_summary, source_url, value_theme, applicable_scene, keywords, difficulty, status, created_by) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展视频思政素材01', 'video', '视频思政素材第01条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '爱国主义', '课堂导入', '视频素材,思政引导,扩展01', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展视频思政素材02', 'video', '视频思政素材第02条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '创新精神', '案例分析', '视频素材,思政引导,扩展02', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展视频思政素材03', 'video', '视频思政素材第03条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '团队协作', '实践应用', '视频素材,思政引导,扩展03', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展视频思政素材04', 'video', '视频思政素材第04条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '社会责任', '课后拓展', '视频素材,思政引导,扩展04', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '扩展视频思政素材05', 'video', '视频思政素材第05条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '文化自信', '课堂导入', '视频素材,思政引导,扩展05', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展视频思政素材06', 'video', '视频思政素材第06条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '职业道德', '案例分析', '视频素材,思政引导,扩展06', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展视频思政素材07', 'video', '视频思政素材第07条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '爱国主义', '实践应用', '视频素材,思政引导,扩展07', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展视频思政素材08', 'video', '视频思政素材第08条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '创新精神', '课后拓展', '视频素材,思政引导,扩展08', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展视频思政素材09', 'video', '视频思政素材第09条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '团队协作', '课堂导入', '视频素材,思政引导,扩展09', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
(NULL, '扩展视频思政素材10', 'video', '视频思政素材第10条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '社会责任', '案例分析', '视频素材,思政引导,扩展10', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展视频思政素材11', 'video', '视频思政素材第11条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '文化自信', '实践应用', '视频素材,思政引导,扩展11', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展视频思政素材12', 'video', '视频思政素材第12条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '职业道德', '课后拓展', '视频素材,思政引导,扩展12', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展视频思政素材13', 'video', '视频思政素材第13条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '爱国主义', '课堂导入', '视频素材,思政引导,扩展13', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展视频思政素材14', 'video', '视频思政素材第14条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '创新精神', '案例分析', '视频素材,思政引导,扩展14', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
(NULL, '扩展视频思政素材15', 'video', '视频思政素材第15条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '团队协作', '实践应用', '视频素材,思政引导,扩展15', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展视频思政素材16', 'video', '视频思政素材第16条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '社会责任', '课后拓展', '视频素材,思政引导,扩展16', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展视频思政素材17', 'video', '视频思政素材第17条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '文化自信', '课堂导入', '视频素材,思政引导,扩展17', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展视频思政素材18', 'video', '视频思政素材第18条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '职业道德', '案例分析', '视频素材,思政引导,扩展18', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展视频思政素材19', 'video', '视频思政素材第19条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '爱国主义', '实践应用', '视频素材,思政引导,扩展19', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '扩展视频思政素材20', 'video', '视频思政素材第20条，围绕课程内容补充思政引导要点，适合后续视频资源上传后继续完善。', NULL, '创新精神', '课后拓展', '视频素材,思政引导,扩展20', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展文档思政案例01', 'document', '文档思政案例第01条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_21.pdf', '团队协作', '课堂导入', '文档案例,课程资源,扩展21', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展文档思政案例02', 'document', '文档思政案例第02条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_22.pdf', '社会责任', '案例分析', '文档案例,课程资源,扩展22', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展文档思政案例03', 'document', '文档思政案例第03条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_23.pdf', '文化自信', '实践应用', '文档案例,课程资源,扩展23', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展文档思政案例04', 'document', '文档思政案例第04条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_24.pdf', '职业道德', '课后拓展', '文档案例,课程资源,扩展24', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
(NULL, '扩展文档思政案例05', 'document', '文档思政案例第05条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_25.pdf', '爱国主义', '课堂导入', '文档案例,课程资源,扩展25', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展文档思政案例06', 'document', '文档思政案例第06条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_26.pdf', '创新精神', '案例分析', '文档案例,课程资源,扩展26', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展文档思政案例07', 'document', '文档思政案例第07条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_27.pdf', '团队协作', '实践应用', '文档案例,课程资源,扩展27', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展文档思政案例08', 'document', '文档思政案例第08条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_28.pdf', '社会责任', '课后拓展', '文档案例,课程资源,扩展28', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展文档思政案例09', 'document', '文档思政案例第09条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_29.pdf', '文化自信', '课堂导入', '文档案例,课程资源,扩展29', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
(NULL, '扩展文档思政案例10', 'document', '文档思政案例第10条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_30.pdf', '职业道德', '案例分析', '文档案例,课程资源,扩展30', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展文档思政案例11', 'document', '文档思政案例第11条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_31.pdf', '爱国主义', '实践应用', '文档案例,课程资源,扩展31', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展文档思政案例12', 'document', '文档思政案例第12条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_32.pdf', '创新精神', '课后拓展', '文档案例,课程资源,扩展32', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展文档思政案例13', 'document', '文档思政案例第13条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_33.pdf', '团队协作', '课堂导入', '文档案例,课程资源,扩展33', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展文档思政案例14', 'document', '文档思政案例第14条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_34.pdf', '社会责任', '案例分析', '文档案例,课程资源,扩展34', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '扩展文档思政案例15', 'document', '文档思政案例第15条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_35.pdf', '文化自信', '实践应用', '文档案例,课程资源,扩展35', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展文档思政案例16', 'document', '文档思政案例第16条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_36.pdf', '职业道德', '课后拓展', '文档案例,课程资源,扩展36', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展文档思政案例17', 'document', '文档思政案例第17条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_37.pdf', '爱国主义', '课堂导入', '文档案例,课程资源,扩展37', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展文档思政案例18', 'document', '文档思政案例第18条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_38.pdf', '创新精神', '案例分析', '文档案例,课程资源,扩展38', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展文档思政案例19', 'document', '文档思政案例第19条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_39.pdf', '团队协作', '实践应用', '文档案例,课程资源,扩展39', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
(NULL, '扩展文档思政案例20', 'document', '文档思政案例第20条，用于补充课程资源库的文本型教学素材。', '/ideology/extended_doc_40.pdf', '社会责任', '课后拓展', '文档案例,课程资源,扩展40', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展案例思政资源01', 'case', '案例型思政资源第01条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_41.pdf', '文化自信', '课堂导入', '案例资源,课程思政,扩展41', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展案例思政资源02', 'case', '案例型思政资源第02条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_42.pdf', '职业道德', '案例分析', '案例资源,课程思政,扩展42', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展案例思政资源03', 'case', '案例型思政资源第03条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_43.pdf', '爱国主义', '实践应用', '案例资源,课程思政,扩展43', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展案例思政资源04', 'case', '案例型思政资源第04条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_44.pdf', '创新精神', '课后拓展', '案例资源,课程思政,扩展44', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
(NULL, '扩展案例思政资源05', 'case', '案例型思政资源第05条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_45.pdf', '团队协作', '课堂导入', '案例资源,课程思政,扩展45', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展案例思政资源06', 'case', '案例型思政资源第06条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_46.pdf', '社会责任', '案例分析', '案例资源,课程思政,扩展46', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展案例思政资源07', 'case', '案例型思政资源第07条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_47.pdf', '文化自信', '实践应用', '案例资源,课程思政,扩展47', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展案例思政资源08', 'case', '案例型思政资源第08条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_48.pdf', '职业道德', '课后拓展', '案例资源,课程思政,扩展48', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展案例思政资源09', 'case', '案例型思政资源第09条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_49.pdf', '爱国主义', '课堂导入', '案例资源,课程思政,扩展49', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
(NULL, '扩展案例思政资源10', 'case', '案例型思政资源第10条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_50.pdf', '创新精神', '案例分析', '案例资源,课程思政,扩展50', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展案例思政资源11', 'case', '案例型思政资源第11条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_51.pdf', '团队协作', '实践应用', '案例资源,课程思政,扩展51', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展案例思政资源12', 'case', '案例型思政资源第12条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_52.pdf', '社会责任', '课后拓展', '案例资源,课程思政,扩展52', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展案例思政资源13', 'case', '案例型思政资源第13条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_53.pdf', '文化自信', '课堂导入', '案例资源,课程思政,扩展53', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展案例思政资源14', 'case', '案例型思政资源第14条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_54.pdf', '职业道德', '案例分析', '案例资源,课程思政,扩展54', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
(NULL, '扩展案例思政资源15', 'case', '案例型思政资源第15条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_55.pdf', '爱国主义', '实践应用', '案例资源,课程思政,扩展55', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展案例思政资源16', 'case', '案例型思政资源第16条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_56.pdf', '创新精神', '课后拓展', '案例资源,课程思政,扩展56', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展案例思政资源17', 'case', '案例型思政资源第17条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_57.pdf', '团队协作', '课堂导入', '案例资源,课程思政,扩展57', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展案例思政资源18', 'case', '案例型思政资源第18条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_58.pdf', '社会责任', '案例分析', '案例资源,课程思政,扩展58', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展案例思政资源19', 'case', '案例型思政资源第19条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_59.pdf', '文化自信', '实践应用', '案例资源,课程思政,扩展59', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
(NULL, '扩展案例思政资源20', 'case', '案例型思政资源第20条，聚焦教学场景中的价值引导与课程融合。', '/ideology/extended_case_60.pdf', '职业道德', '课后拓展', '案例资源,课程思政,扩展60', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展视频思政素材21', 'video', '视频思政素材第21条，继续补充课程视频场景下的思政引导内容。', NULL, '爱国主义', '课堂导入', '视频素材,思政引导,扩展21', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展视频思政素材22', 'video', '视频思政素材第22条，继续补充课程视频场景下的思政引导内容。', NULL, '创新精神', '案例分析', '视频素材,思政引导,扩展22', 'medium', 'draft', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展视频思政素材23', 'video', '视频思政素材第23条，继续补充课程视频场景下的思政引导内容。', NULL, '团队协作', '实践应用', '视频素材,思政引导,扩展23', 'hard', 'draft', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展视频思政素材24', 'video', '视频思政素材第24条，继续补充课程视频场景下的思政引导内容。', NULL, '社会责任', '课后拓展', '视频素材,思政引导,扩展24', 'easy', 'draft', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), '扩展文档思政案例21', 'document', '文档思政案例第21条，继续补充课程资源库中的文本型素材。', '/ideology/extended_doc_61.pdf', '文化自信', '课堂导入', '文档案例,课程资源,扩展61', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2')),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), '扩展文档思政案例22', 'document', '文档思政案例第22条，继续补充课程资源库中的文本型素材。', '/ideology/extended_doc_62.pdf', '职业道德', '案例分析', '文档案例,课程资源,扩展62', 'hard', 'published', (SELECT id FROM users WHERE username = 'teacher3')),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), '扩展案例思政资源21', 'case', '案例型思政资源第21条，继续补充课程资源库中的课程融合素材。', '/ideology/extended_case_63.pdf', '爱国主义', '实践应用', '案例资源,课程思政,扩展63', 'easy', 'published', (SELECT id FROM users WHERE username = 'teacher')),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), '扩展案例思政资源22', 'case', '案例型思政资源第22条，继续补充课程资源库中的课程融合素材。', '/ideology/extended_case_64.pdf', '创新精神', '课后拓展', '案例资源,课程思政,扩展64', 'medium', 'published', (SELECT id FROM users WHERE username = 'teacher2'));

-- 资源标签关联
INSERT INTO ideology_resource_tag_rel (resource_id, tag_id) VALUES
((SELECT resource_id FROM ideology_resources WHERE title = '中国计算机发展历程'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '爱国主义')),
((SELECT resource_id FROM ideology_resources WHERE title = '中国计算机发展历程'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '课堂导入')),
((SELECT resource_id FROM ideology_resources WHERE title = '算法与公平正义'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '社会责任')),
((SELECT resource_id FROM ideology_resources WHERE title = '算法与公平正义'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '案例分析')),
((SELECT resource_id FROM ideology_resources WHERE title = '软件工程师的职业操守'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '职业道德')),
((SELECT resource_id FROM ideology_resources WHERE title = 'AI伦理与社会责任'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '社会责任')),
((SELECT resource_id FROM ideology_resources WHERE title = '中国人工智能发展战略'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '爱国主义')),
((SELECT resource_id FROM ideology_resources WHERE title = '开源精神与协作文化'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '团队协作')),
((SELECT resource_id FROM ideology_resources WHERE title = '深度学习与创新精神'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '创新精神')),
((SELECT resource_id FROM ideology_resources WHERE title = 'AI赋能传统文化'), (SELECT tag_id FROM ideology_resource_tags WHERE tag_name = '文化自信'));

-- 思政资源推荐记录
INSERT INTO ideology_resource_recommendation (student_id, resource_id, course_id, reason, score, recommendation_type, has_clicked) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT resource_id FROM ideology_resources WHERE title = '中国计算机发展历程'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), '基于您学习的计算机科学课程推荐', 0.95, 'PROGRESS_SCENE', TRUE),
((SELECT id FROM users WHERE username = 'student'), (SELECT resource_id FROM ideology_resources WHERE title = '算法与公平正义'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), '数据结构课程相关思政资源', 0.88, 'PROGRESS_SCENE', FALSE),
((SELECT id FROM users WHERE username = 'student2'), (SELECT resource_id FROM ideology_resources WHERE title = '软件工程师的职业操守'), NULL, '热门推荐', 0.75, 'POPULAR', TRUE),
((SELECT id FROM users WHERE username = 'student3'), (SELECT resource_id FROM ideology_resources WHERE title = 'AI伦理与社会责任'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), 'AI课程相关推荐', 0.92, 'TAG_MATCH', FALSE),
((SELECT id FROM users WHERE username = 'student'), (SELECT resource_id FROM ideology_resources WHERE title = '开源精神与协作文化'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), '标签匹配推荐', 0.82, 'TAG_MATCH', TRUE);

-- ================================================
-- 教学评价测试数据
-- ================================================

INSERT INTO teaching_evaluations (course_id, student_id, teacher_id, content_integration_score, interaction_score, participation_score, value_recognition_score, total_score, evaluation_period, evaluation_type, remark) VALUES
((SELECT course_id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'student'), (SELECT id FROM users WHERE username = 'teacher'), 85.0, 78.0, 82.0, 75.0, 80.0, 'monthly', 'student', '课程融入了丰富的思政案例'),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'student2'), (SELECT id FROM users WHERE username = 'teacher'), 72.0, 68.0, 65.0, 70.0, 68.75, 'monthly', 'student', '教学效果好，建议增加互动'),
((SELECT course_id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'student3'), (SELECT id FROM users WHERE username = 'teacher'), 90.0, 85.0, 88.0, 86.0, 87.25, 'monthly', 'student', '课程设计优秀，思政融入自然'),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), (SELECT id FROM users WHERE username = 'student'), (SELECT id FROM users WHERE username = 'teacher'), 78.0, 75.0, 70.0, 72.0, 73.75, 'monthly', 'student', '理论与实践结合紧密'),
((SELECT course_id FROM courses WHERE course_code = 'CS201'), (SELECT id FROM users WHERE username = 'student3'), (SELECT id FROM users WHERE username = 'teacher'), 82.0, 80.0, 78.0, 75.0, 78.75, 'monthly', 'student', '算法案例分析生动'),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), (SELECT id FROM users WHERE username = 'student2'), (SELECT id FROM users WHERE username = 'teacher2'), 88.0, 82.0, 85.0, 80.0, 83.75, 'monthly', 'student', '软件工程实践性强'),
((SELECT course_id FROM courses WHERE course_code = 'SE101'), (SELECT id FROM users WHERE username = 'student4'), (SELECT id FROM users WHERE username = 'teacher2'), 75.0, 70.0, 68.0, 72.0, 71.25, 'monthly', 'student', '课程内容丰富'),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), (SELECT id FROM users WHERE username = 'student5'), (SELECT id FROM users WHERE username = 'teacher3'), 92.0, 88.0, 85.0, 90.0, 88.75, 'monthly', 'student', 'AI伦理讨论深入'),
((SELECT course_id FROM courses WHERE course_code = 'AI101'), (SELECT id FROM users WHERE username = 'student6'), (SELECT id FROM users WHERE username = 'teacher3'), 78.0, 75.0, 72.0, 76.0, 75.25, 'monthly', 'student', '理论与实践结合好');

-- ================================================
-- 学生行为画像测试数据
-- ================================================

INSERT INTO student_behavior_profiles (student_id, preferred_study_time, morning_ratio, afternoon_ratio, evening_ratio, preferred_resource_type, video_preference, document_preference, interactive_preference, avg_session_duration, interaction_frequency, completion_rate, consistency_score, learning_style_tag, engagement_level, ideology_click_rate, ideology_completion_rate) VALUES
((SELECT id FROM users WHERE username = 'student'), 'evening', 0.15, 0.25, 0.60, 'video', 0.65, 0.25, 0.10, 45.5, 12, 0.78, 0.85, 'visual', 'high', 0.72, 0.65),
((SELECT id FROM users WHERE username = 'student2'), 'afternoon', 0.20, 0.55, 0.25, 'document', 0.30, 0.55, 0.15, 32.0, 8, 0.65, 0.72, 'auditory', 'medium', 0.45, 0.38),
((SELECT id FROM users WHERE username = 'student3'), 'morning', 0.50, 0.30, 0.20, 'mixed', 0.40, 0.35, 0.25, 55.0, 15, 0.88, 0.92, 'kinesthetic', 'high', 0.85, 0.78),
((SELECT id FROM users WHERE username = 'student4'), 'evening', 0.10, 0.30, 0.60, 'interactive', 0.35, 0.30, 0.35, 38.5, 10, 0.70, 0.68, 'kinesthetic', 'medium', 0.52, 0.45),
((SELECT id FROM users WHERE username = 'student5'), 'afternoon', 0.25, 0.50, 0.25, 'video', 0.70, 0.20, 0.10, 42.0, 11, 0.75, 0.78, 'visual', 'high', 0.68, 0.60),
((SELECT id FROM users WHERE username = 'student6'), 'evening', 0.15, 0.35, 0.50, 'document', 0.25, 0.60, 0.15, 35.0, 7, 0.62, 0.65, 'auditory', 'medium', 0.40, 0.35);

-- ================================================
-- 学习路径记录测试数据
-- ================================================

INSERT INTO learning_path_records (student_id, course_id, resource_type, resource_id, resource_title, action_type, duration_seconds, progress_percent, device_type) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'video', 1, '计算机发展历史概述', 'complete', 1800, 100, 'pc'),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'video', 2, '冯·诺依曼体系结构', 'view', 1500, 71, 'pc'),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'ideology', (SELECT resource_id FROM ideology_resources WHERE title = '中国计算机发展历程'), '中国计算机发展历程', 'complete', 1200, 100, 'pc'),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), 'document', 1, '数据结构与算法讲义', 'view', 900, 45, 'mobile'),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'video', 1, '计算机发展历史概述', 'view', 1200, 67, 'pc'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'video', 1, '计算机发展历史概述', 'complete', 1800, 100, 'pc'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'ideoology', (SELECT resource_id FROM ideology_resources WHERE title = '图灵奖华人得主介绍'), '图灵奖华人得主介绍', 'complete', 900, 100, 'pc'),
((SELECT id FROM users WHERE username = 'student5'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), 'ideoology', (SELECT resource_id FROM ideology_resources WHERE title = 'AI伦理与社会责任'), 'AI伦理与社会责任', 'view', 600, 50, 'mobile');

-- ================================================
-- 情感分析记录测试数据
-- ================================================

INSERT INTO sentiment_records (student_id, course_id, sentiment_type, positive_score, negative_score, neutral_score, source_type, source_content, ideology_related, ideology_theme) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'positive', 0.72, 0.08, 0.20, 'chat', '我觉得这门课很有趣，老师讲解很清晰', TRUE, '学习兴趣'),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), 'positive', 0.65, 0.15, 0.20, 'chat', '数据结构虽然难，但是很有用', FALSE, NULL),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'neutral', 0.35, 0.25, 0.40, 'chat', '课程内容还可以，希望能有更多实践', FALSE, NULL),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 'positive', 0.85, 0.05, 0.10, 'chat', '非常感谢老师的耐心指导，收获很大！', TRUE, '感恩之心'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), 'positive', 0.78, 0.10, 0.12, 'discussion', '算法设计让我感受到了编程之美', TRUE, '工匠精神'),
((SELECT id FROM users WHERE username = 'student5'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), 'positive', 0.80, 0.08, 0.12, 'chat', 'AI伦理的讨论让我对技术有了更深的思考', TRUE, '社会责任'),
((SELECT id FROM users WHERE username = 'student6'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), 'neutral', 0.40, 0.30, 0.30, 'chat', '深度学习的内容有点难理解', FALSE, NULL);

-- ================================================
-- 价值认同评估测试数据
-- ================================================

INSERT INTO value_assessments (student_id, course_id, patriotism_score, social_responsibility_score, professional_ethics_score, innovation_score, cultural_confidence_score, total_score, assessment_level, previous_score, trend_direction, trend_value, assessment_basis, assessment_period) VALUES
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 88.0, 82.0, 85.0, 78.0, 80.0, 82.6, 'high', 78.0, 'up', 4.6, '基于学习行为和互动数据分析', 'monthly'),
((SELECT id FROM users WHERE username = 'student'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), 75.0, 78.0, 80.0, 82.0, 72.0, 77.4, 'medium', 75.0, 'up', 2.4, '算法学习中展现出创新意识', 'monthly'),
((SELECT id FROM users WHERE username = 'student2'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 70.0, 68.0, 72.0, 65.0, 68.0, 68.6, 'medium', 70.0, 'down', 1.4, '学习积极性有所下降', 'monthly'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS101'), 92.0, 88.0, 90.0, 85.0, 88.0, 88.6, 'high', 85.0, 'up', 3.6, '表现出强烈的求知欲和责任感', 'monthly'),
((SELECT id FROM users WHERE username = 'student3'), (SELECT course_id FROM courses WHERE course_code = 'CS201'), 88.0, 85.0, 88.0, 90.0, 82.0, 86.6, 'high', 82.0, 'up', 4.6, '在算法学习中体现出工匠精神', 'monthly'),
((SELECT id FROM users WHERE username = 'student5'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), 85.0, 90.0, 82.0, 88.0, 80.0, 85.0, 'high', 80.0, 'up', 5.0, '对AI伦理有深入思考', 'monthly'),
((SELECT id FROM users WHERE username = 'student6'), (SELECT course_id FROM courses WHERE course_code = 'AI101'), 72.0, 70.0, 75.0, 68.0, 70.0, 71.0, 'medium', 72.0, 'stable', 1.0, '学习态度稳定', 'monthly');

-- ================================================
-- 教师授课风格画像测试数据
-- ================================================

INSERT INTO teaching_style_profiles (teacher_id, interaction_level, content_depth, practicality, innovation, ideology_integration, ideology_approach, main_value_themes, ideology_resource_count, avg_student_satisfaction, avg_engagement_rate, style_tag) VALUES
((SELECT id FROM users WHERE username = 'teacher'), 82.0, 88.0, 75.0, 78.0, 85.0, 'explicit', '爱国主义,创新精神,科学精神', 8, 82.5, 78.0, 'interactive'),
((SELECT id FROM users WHERE username = 'teacher2'), 75.0, 82.0, 88.0, 72.0, 78.0, 'mixed', '职业道德,团队协作', 6, 80.0, 75.0, 'practical'),
((SELECT id FROM users WHERE username = 'teacher3'), 78.0, 85.0, 70.0, 90.0, 82.0, 'explicit', '社会责任,创新精神,文化自信', 7, 85.0, 80.0, 'innovative');
