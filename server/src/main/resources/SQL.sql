
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
-- ================================================
-- 测试账号数据
-- ================================================

-- 默认账号（项目说明中使用的基础账号）
INSERT INTO users (username, password, role) VALUES
('student', '123456', 'student'),
('teacher', '123456', 'teacher');

INSERT INTO students (id, student_number, name, class_name, email, phone, major, grade, enrollment_year, status, group_status) VALUES
((SELECT id FROM users WHERE username = 'student'), '2024001', '王小明', '思政24-1班', 'wangxiaoming@student.ccut.edu.cn', '13812345678', '思想政治教育', '2024级', 2024, 'IN_SCHOOL', 'pending');

INSERT INTO teachers (id, employee_number, name, email, phone, department, title, position, bio) VALUES
((SELECT id FROM users WHERE username = 'teacher'), 'T2024001', '张教授', 'zhang@teacher.ccut.edu.cn', '13987654321', '马克思主义学院', '教授', '课程负责人', '主要研究方向：马克思主义理论、思想政治教育。');

