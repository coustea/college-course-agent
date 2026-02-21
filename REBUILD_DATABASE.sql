-- 数据库快速重建脚本
-- 在IntelliJ IDEA的Database控制台中执行此脚本

SET FOREIGN_KEY_CHECKS = 0;

-- 删除所有表
DROP TABLE IF EXISTS wrong_question;
DROP TABLE IF EXISTS ai_exam_answers;
DROP TABLE IF EXISTS ai_exam_attempts;
DROP TABLE IF EXISTS ai_exam_questions;
DROP TABLE IF EXISTS ai_exams;
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

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'All tables dropped. Now run the full SQL.sql file.' AS status;
