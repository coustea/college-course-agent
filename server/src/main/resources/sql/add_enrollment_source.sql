-- 为现有enrollments表添加enrollment_source字段
-- 如果字段已存在则忽略

ALTER TABLE enrollments
ADD COLUMN IF NOT EXISTS enrollment_source VARCHAR(20) DEFAULT 'student'
COMMENT '选课来源：teacher(教师强制添加/必修) / student(学生自选/选修)';

-- 更新现有记录（可选：将所有现有记录设为教师添加，或根据实际情况调整）
-- UPDATE enrollments SET enrollment_source = 'teacher' WHERE enrollment_source IS NULL;
