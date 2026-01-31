# 后端项目代码问题分析报告

> 生成时间: 2026-01-31
> 项目路径: /home/couseta/develop/CCUT/backend
> 分析范围: 全部Java源代码

---

## 问题优先级说明

| 优先级 | 说明 | 标记 |
|--------|------|------|
| 🔴 高 | 安全风险、数据丢失、系统崩溃 | 需立即修复 |
| 🟡 中 | 功能缺陷、性能问题、维护性 | 近期修复 |
| 🟢 低 | 代码规范、轻微优化 | 长期优化 |

---

## 🔴 高优先级问题 (需立即修复)


#### 1.2 密码明文存储
**文件**: `src/main/java/com/ccut/service/Impl/AuthServiceImpl.java`
- **第54行**: 直接比较明文密码 `if (!user.getPassword().equals(dbUser.getPassword()))`
- **影响**: 数据库泄露将导致所有账户被盗
- **建议**: 使用BCrypt等加密算法存储密码

#### 1.3 JWT密钥硬编码
**文件**: `src/main/java/com/ccut/utils/JWTUtils.java`
- **第21行**: 签名密钥硬编码 `SIGN_KEY = "HSyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"`
- **影响**: 密钥泄露可伪造任意token
- **建议**: 从配置中心读取密钥

#### 1.4 文件上传无类型验证
**文件**: `src/main/java/com/ccut/controller/UploadController.java`
- **第25行**: 没有验证文件类型
- **影响**: 可上传恶意文件（.exe, .sh, .php等）
- **建议**: 实现文件类型白名单

#### 1.5 路径遍历风险
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第103行**: 文件扩展名直接使用用户输入，无验证
- **影响**: 可能导致任意文件写入
- **建议**: 验证并过滤文件扩展名

### 2. 数据一致性问题

#### 2.1 文件上传与数据库事务不一致
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第46-57行**: 文件上传成功但数据库插入失败时，产生孤立文件
- **影响**: 磁盘空间浪费，数据不一致
- **建议**: 使用事务确保文件操作和数据库操作的原子性

#### 2.2 考试分数更新失败被忽略
**文件**: `src/main/java/com/ccut/service/Impl/AiExamServiceImpl.java`
- **第159行**: `try { attemptMapper.updateScore(...) } catch (Exception ignore) {}`
- **影响**: 分数可能不更新，用户看不到正确成绩
- **建议**: 记录失败日志，实现重试机制

#### 2.3 Redis缓存与MySQL不一致
**文件**: `src/main/java/com/ccut/service/Impl/MessageServiceImpl.java`
- **第111-118行**: 异步刷新缓存，可能与MySQL数据不一致
- **影响**: 用户看到过期数据
- **建议**: 实现缓存失效策略或版本控制

### 3. 性能问题

#### 3.1 N+1查询问题
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第121-132行**: 先查询所有课程，循环中查询视频和文档
```java
for (Course c : courses) {
    c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
    c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
}
```
- **影响**: 当课程数量多时，数据库压力巨大
- **建议**: 使用JOIN查询或批量查询

#### 3.2 缺少查询结果缓存
**文件**: `src/main/java/com/ccut/service/Impl/ChatServiceImpl.java`
- **第95行**: 每次聊天都从数据库加载历史消息
- **影响**: 高并发时数据库压力大
- **建议**: 实现多级缓存

### 4. 资源泄露

#### 4.1 分片上传内存泄露
**文件**: `src/main/java/com/ccut/controller/ChunkUploadController.java`
- **chunkInfoMap**: 没有设置过期清理机制
- **影响**: 分片信息长期驻留内存，可能OOM
- **建议**: 实现定时清理或使用Guava Cache

---

## 🟡 中优先级问题 (近期修复)

### 1. 业务逻辑缺陷

#### 1.1 除零逻辑不清晰
**文件**: `src/main/java/com/ccut/service/Impl/AiExamServiceImpl.java`
- **第139行**: `int per = 100 / Math.max(1, qs.size())`
- **问题**: 虽然避免了除零，但没有明确处理题目数为0的情况
- **建议**: 单独判断 `qs.isEmpty()`

#### 1.2 边界条件处理不完整
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第102-104行**: 文件扩展名处理时，没有`.`或`.`在开头的情况
- **影响**: 可能生成无效文件名
- **建议**: 添加文件名有效性验证

#### 1.3 空值检查不完整
**文件**: `src/main/java/com/ccut/service/Impl/UserServiceImpl.java`
- **第17-34行**: 所有方法都没有输入参数空值检查
- **影响**: 可能导致NullPointerException
- **建议**: 添加 `@NonNull` 注解或显式检查

#### 1.4 参数范围验证缺失
**文件**: `src/main/java/com/ccut/controller/AiExamController.java`
- **第65-66行**: `@RequestParam` 只检查null，没检查负数或0
- **影响**: 可能产生无效的业务数据
- **建议**: 添加 `@Min(1)` 等验证注解

### 2. 事务边界不清晰

#### 2.1 事务粒度问题
**文件**: `src/main/java/com/ccut/service/Impl/AiExamServiceImpl.java`
- **第73-126行**: `@Transactional` 方法内有多个独立操作
- **影响**: 事务时间过长，锁竞争
- **建议**: 拆分事务，明确边界

#### 2.2 异步操作无事务支持
**文件**: `src/main/java/com/ccut/service/Impl/MessageServiceImpl.java`
- **第137-149行**: 异步方法没有事务管理
- **影响**: 可能导致缓存与数据库不一致
- **建议**: 确保异步操作不影响数据一致性

### 3. 异常处理不规范

#### 3.1 错误码使用不一致
**文件**: 多个Controller
- **问题**: 业务异常使用404状态码，应该用400
- **影响**: 客户端难以正确处理错误
- **建议**: 统一使用错误码枚举

#### 3.2 异常信息暴露系统细节
**文件**: `src/main/java/com/ccut/exception/GlobalExceptionHandler.java`
- **第85行**: 返回 `e.getMessage()` 给客户端
- **影响**: 可能暴露系统内部结构
- **建议**: 对外返回友好信息，详细日志记录到服务器

#### 3.3 异常类型使用不当
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第56行**: `throw new RuntimeException("添加失败")`
- **影响**: 无法区分不同类型的业务异常
- **建议**: 创建自定义业务异常类

### 4. 架构设计问题

#### 4.1 职责不清
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第46-86行**: 既处理业务逻辑又处理文件上传
- **影响**: 违反单一职责原则
- **建议**: 抽取FileService处理文件操作

#### 4.2 代码重复
**文件**: `src/main/java/com/ccut/service/Impl/CourseServiceImpl.java`
- **第126、138、154行**: 重复调用相同的Mapper方法
- **影响**: 维护困难
- **建议**: 抽取为私有方法

#### 4.3 方法过长
**文件**: `src/main/java/com/ccut/controller/UserController.java`
- **第54-158行**: `insertByExcel` 方法104行
- **影响**: 难以理解和维护
- **建议**: 拆分为多个小方法

### 5. 外部调用问题

#### 5.1 无超时机制
**文件**: AI相关调用
- **问题**: DashScope API调用没有设置超时
- **影响**: 可能导致线程永久阻塞
- **建议**: 设置合理的超时时间

#### 5.2 无重试机制
**文件**: 所有外部调用
- **问题**: 网络抖动时服务不可用
- **影响**: 用户体验差
- **建议**: 实现指数退避重试

---

## 🟢 低优先级问题 (长期优化)

### 1. 代码规范

#### 1.1 命名不规范
**文件**: 多个文件
- **问题**: 方法命名不一致，有的用驼峰，有的用下划线
- **建议**: 统一使用驼峰命名

#### 1.2 注释不足
**文件**: `src/main/java/com/ccut/service/Impl/UserServiceImpl.java`
- **问题**: 类和方法缺少注释
- **建议**: 添加JavaDoc注释

#### 1.3 魔法数字
**文件**: `src/main/java/com/ccut/utils/JWTUtils.java`
- **第18行**: `private static final long EXPIRE = 2 * 60 * 60 * 1000`
- **建议**: 提取为常量并添加注释

### 2. 日志问题

#### 2.1 使用System.out.println
**文件**: `src/main/java/com/ccut/controller/UserController.java`
- **第138行**: `System.out.println(student)`
- **影响**: 生产环境无法控制日志输出
- **建议**: 使用slf4j日志框架

#### 2.2 日志级别不当
**文件**: 多个Controller
- **问题**: 使用 `log.error` 记录业务警告
- **建议**: 正确使用日志级别

### 3. 并发优化

#### 3.1 线程池配置
**文件**: `src/main/java/com/ccut/config/AsyncConfig.java`
- **第34、54行**: 使用 `CallerRunsPolicy` 可能阻塞主线程
- **建议**: 考虑使用 `AbortPolicy` 并实现降级逻辑

#### 3.2 缺少监控
**问题**: 没有线程池监控指标
- **建议**: 添加线程池监控和告警

---

## 问题统计

| 类别 | 高优先级 | 中优先级 | 低优先级 | 合计 |
|------|---------|---------|---------|------|
| 安全问题 | 5 | 0 | 0 | 5 |
| 数据一致性 | 3 | 0 | 0 | 3 |
| 性能问题 | 2 | 0 | 0 | 2 |
| 资源管理 | 1 | 0 | 0 | 1 |
| 业务逻辑 | 0 | 4 | 0 | 4 |
| 异常处理 | 0 | 3 | 0 | 3 |
| 架构设计 | 0 | 3 | 0 | 3 |
| 外部调用 | 0 | 2 | 0 | 2 |
| 代码规范 | 0 | 0 | 3 | 3 |
| 日志问题 | 0 | 0 | 2 | 2 |
| 并发优化 | 0 | 0 | 2 | 2 |
| **总计** | **11** | **12** | **7** | **30** |

---

## 修复建议优先级

### 第一阶段 (立即修复，1周内)
1. 移除application.yml中的敏感信息，使用环境变量
2. 实现密码加密存储
3. 启用JWT认证（生产环境）
4. 添加文件上传类型验证
5. 修复N+1查询问题

### 第二阶段 (近期修复，2周内)
1. 完善参数校验和边界条件处理
2. 统一异常处理和错误码
3. 修复文件上传与数据库事务一致性
4. 实现缓存失效策略
5. 添加外部调用超时和重试

### 第三阶段 (长期优化，1个月内)
1. 重构过长方法和重复代码
2. 抽取FileService，优化架构设计
3. 添加日志监控和告警
4. 完善单元测试覆盖
5. 代码规范统一

---

## 工具建议

建议使用以下工具进行自动化代码质量检查：

1. **SonarQube** - 代码质量分析
2. **Find Security Bugs** - 安全漏洞扫描
3. **SpotBugs** - Bug检测
4. **CheckStyle** - 代码规范检查
5. **JaCoCo** - 测试覆盖率统计