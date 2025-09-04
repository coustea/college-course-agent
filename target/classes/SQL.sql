CREATE TABLE IF NOT EXISTS admin (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  password VARCHAR(128),
  name VARCHAR(64),
  role VARCHAR(32),
  token VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS student (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  password VARCHAR(128),
  academe VARCHAR(64),
  classname VARCHAR(64),
  name VARCHAR(64),
  phone VARCHAR(32),
  role VARCHAR(32),
  token VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS teacher (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  password VARCHAR(128),
  academe VARCHAR(64),
  name VARCHAR(64),
  phone VARCHAR(32),
  role VARCHAR(32),
  token VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS course (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(128),
  term VARCHAR(64),
  status INT
);

CREATE TABLE IF NOT EXISTS course_teacher (
  course_id INT,
  teacher_id INT
);

CREATE TABLE IF NOT EXISTS course_resource (
  id INT AUTO_INCREMENT PRIMARY KEY,
  course_id INT,
  ppt_path VARCHAR(255),
  image_path VARCHAR(255),
  video_path VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS learning_progress (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT,
  course_id BIGINT,
  completion_percentage DECIMAL(5,2),
  status INT
);

