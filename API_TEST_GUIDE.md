# 后端API接口测试指南

## 项目配置

- **服务器地址**: `http://localhost:9999`
- **JWT认证**: 已关闭 (`jwt.enabled: false`)
- **数据库**: MySQL (localhost:3306/ccut)
- **Redis**: localhost:6379

## 启动项目

### 方法1: 使用Maven
```bash
cd /home/couseta/develop/CCUT/backend
mvn spring-boot:run
```

### 方法2: 使用IDE
- 在IDEA中打开项目
- 找到 `ServiceApplication.java`
- 右键运行 `main()` 方法

### 方法3: 使用jar包
```bash
# 先打包
mvn clean package -DskipTests

# 运行
java -jar target/service-0.0.1-SNAPSHOT.jar
```

## 运行测试脚本

### 基本用法
```bash
cd /home/couseta/develop/CCUT/backend
./test_api.sh
```

### 查看实时测试结果
```bash
./test_api.sh 2>&1 | tee test_result.log
```

## 测试的API接口列表

### 1. 认证控制器 (4个测试)
- POST `/api/auth/login` - 用户登录

### 2. 用户控制器 (1个测试)
- GET `/api/user/list` - 查询所有用户

### 3. 学生控制器 (2个测试)
- GET `/api/student/by-grade` - 按年级查询学生
- GET `/api/student/class/{className}` - 按班级查询学生

### 4. 教师控制器 (7个测试)
- GET `/api/teacher/classNames` - 获取教师班级列表
- GET `/api/teacher/{id}` - 按ID获取教师
- GET `/api/teacher/list/students` - 列出所有学生
- GET `/api/teacher/list/teachers` - 列出所有教师
- GET `/api/teacher/videos` - 获取教师视频列表

### 5. 课程控制器 (3个测试)
- GET `/api/course/list` - 查询所有课程
- GET `/api/course/detail` - 查询课程详情
- GET `/api/course/search` - 搜索课程

### 6. 学习进度控制器 (6个测试)
- GET `/api/progress/course` - 查询课程汇总进度
- GET `/api/progress/video` - 查询视频进度
- GET `/api/progress/video/list` - 查询视频进度列表
- GET `/api/progress/document` - 查询文档进度
- GET `/api/progress/document/list` - 查询文档进度列表
- GET `/api/progress/course/all` - 查询全部进度

### 7. 课程视频控制器 (1个测试)
- GET `/api/course/video/list` - 按课程列出视频

### 8. 课程文档控制器 (1个测试)
- GET `/api/course/document/list` - 按课程列出文档

### 9. 作业提交控制器 (5个测试)
- GET `/api/submission` - 查看所有作业提交
- GET `/api/submission/{assignmentId}` - 按作业ID查询提交
- GET `/api/submission/detail/{submissionId}` - 获取提交详情
- GET `/api/submission/{submissionId}/comment` - 获取小组评语
- GET `/api/submission/my-group` - 查询小组提交记录

### 10. 教师作业控制器 (3个测试)
- GET `/api/teacherAssignments/{teacherId}` - 按教师ID查询作业
- GET `/api/teacherAssignments/by-course/{courseId}` - 按课程查询作业
- GET `/api/teacherAssignments` - 查询所有作业

### 11. 教师评分控制器 (1个测试)
- GET `/api/grading/group/{submissionId}` - 获取小组评分

### 12. AI考试控制器 (4个测试)
- GET `/api/aiexam/accuracy` - 查询题目正确率
- GET `/api/aiexam/average-score` - 查询平均成绩
- GET `/api/aiexam/detailed-scores` - 查询详细成绩
- GET `/api/aiexam/list` - 查询考试记录

### 13. 会话控制器 (2个测试)
- GET `/api/ai/conversation/list/{username}` - 获取用户会话列表
- GET `/api/ai/conversation/{conversationId}/messages` - 获取会话消息

### 14. 学生小组控制器 (1个测试)
- GET `/api/student-group` - 查询所有小组

## 输出说明

测试脚本会输出彩色结果：
- 🟢 **绿色** [✓ PASS] - 测试通过
- 🔴 **红色** [✗ FAIL] - 测试失败
- 🟡 **黄色** [TEST] - 正在测试

测试结束后会显示：
- 总测试数
- 通过数
- 失败数
- 失败的API列表（如果有）

## 注意事项

1. **确保服务已启动** - 测试前请确保后端服务在9999端口运行
2. **数据库连接** - 确保MySQL和Redis服务正在运行
3. **测试数据** - 脚本使用ID=1进行测试，确保数据库中有对应的测试数据
4. **JWT已关闭** - 当前配置 `jwt.enabled: false`，无需认证

## 完整API接口列表

共19个Controller，89个API接口：

| Controller | 接口数 | 基础路径 |
|-----------|-------|---------|
| AuthController | 2 | /api/auth |
| UserController | 4 | /api/user |
| StudentController | 4 | /api/student |
| TeacherController | 13 | /api/teacher |
| CourseController | 7 | /api/course |
| SubmissionController | 8 | /api/submission |
| TeacherAssignmentController | 7 | /api/teacherAssignments |
| TeacherGradingController | 2 | /api/grading |
| AiController | 1 | /api/ai |
| AiExamController | 6 | /api/aiexam |
| ChatController | 2 | /api/ai/chat |
| ConversationController | 5 | /api/ai/conversation |
| StudentGroupController | 7 | /api/student-group |
| GroupMemberController | 3 | /api/groupMember |
| UploadController | 1 | /api |
| ChunkUploadController | 5 | /api/chunk |
| ProgressController | 7 | /api/progress |
| CourseVideoController | 4 | /api/course/video |
| CourseDocumentController | 4 | /api/course/document |