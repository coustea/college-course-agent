#!/bin/bash

# =====================================================
# 后端API接口测试脚本
# 服务器地址: http://localhost:9999
# JWT认证: 已关闭
# =====================================================

BASE_URL="http://localhost:9999"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试结果记录
declare -a FAILED_APIS

# 打印函数
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓ PASS]${NC} $1"
    ((PASSED_TESTS++))
}

print_fail() {
    echo -e "${RED}[✗ FAIL]${NC} $1"
    echo -e "  ${RED}响应: $2${NC}"
    ((FAILED_TESTS++))
    FAILED_APIS+=("$1")
}

# HTTP请求函数
test_get() {
    local endpoint="$1"
    local description="$2"
    local response
    response=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}${endpoint}")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    ((TOTAL_TESTS++))
    print_test "$description"

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "$description - HTTP $http_code"
        return 0
    else
        print_fail "$description" "HTTP $http_code - $body"
        return 1
    fi
}

test_post() {
    local endpoint="$1"
    local data="$2"
    local description="$3"
    local content_type="$4"

    if [ -z "$content_type" ]; then
        content_type="Content-Type: application/json"
    fi

    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}${endpoint}" \
        -H "$content_type" \
        -d "$data")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    ((TOTAL_TESTS++))
    print_test "$description"

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "$description - HTTP $http_code"
        return 0
    else
        print_fail "$description" "HTTP $http_code - $body"
        return 1
    fi
}

test_put() {
    local endpoint="$1"
    local data="$2"
    local description="$3"
    local content_type="$4"

    if [ -z "$content_type" ]; then
        content_type="Content-Type: application/json"
    fi

    local response
    response=$(curl -s -w "\n%{http_code}" -X PUT "${BASE_URL}${endpoint}" \
        -H "$content_type" \
        -d "$data")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    ((TOTAL_TESTS++))
    print_test "$description"

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "$description - HTTP $http_code"
        return 0
    else
        print_fail "$description" "HTTP $http_code - $body"
        return 1
    fi
}

test_delete() {
    local endpoint="$1"
    local description="$2"
    local response
    response=$(curl -s -w "\n%{http_code}" -X DELETE "${BASE_URL}${endpoint}")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    ((TOTAL_TESTS++))
    print_test "$description"

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "$description - HTTP $http_code"
        return 0
    else
        print_fail "$description" "HTTP $http_code - $body"
        return 1
    fi
}

# =====================================================
# 开始测试
# =====================================================

print_header "后端API接口测试"
echo "服务器地址: $BASE_URL"
echo "测试开始时间: $(date '+%Y-%m-%d %H:%M:%S')"

# =====================================================
# 1. 认证控制器 (AuthController)
# =====================================================
print_header "1. 认证控制器 (AuthController)"

test_post "/api/auth/login" '{"username":"test","password":"123456","role":"student"}' "用户登录"

# =====================================================
# 2. 用户控制器 (UserController)
# =====================================================
print_header "2. 用户控制器 (UserController)"

# UserController 没有 /list 接口，跳过
echo "UserController 没有 /list 接口，跳过测试"

# =====================================================
# 3. 学生控制器 (StudentController)
# =====================================================
print_header "3. 学生控制器 (StudentController)"

test_get "/api/student/by-grade?grade=2023" "按年级查询学生"
test_get "/api/student/class/计算机科学与技术1班" "按班级查询学生"

# =====================================================
# 4. 教师控制器 (TeacherController)
# =====================================================
print_header "4. 教师控制器 (TeacherController)"

test_get "/api/teacher/classNames?teacherId=1" "获取教师班级列表"
test_get "/api/teacher/1" "按ID获取教师"
test_get "/api/teacher/list/students" "列出所有学生"
test_get "/api/teacher/list/teachers" "列出所有教师"
test_get "/api/teacher/videos?teacherId=1" "获取教师视频列表"

# =====================================================
# 5. 课程控制器 (CourseController)
# =====================================================
print_header "5. 课程控制器 (CourseController)"

test_get "/api/course/list" "查询所有课程"
test_get "/api/course/detail?courseId=1" "查询课程详情"
test_get "/api/course/search?name=Java" "搜索课程（按名称）"

# =====================================================
# 6. 学习进度控制器 (ProgressController)
# =====================================================
print_header "6. 学习进度控制器 (ProgressController)"

test_get "/api/progress/course?studentId=1&courseId=1" "查询课程汇总进度"
test_get "/api/progress/video?studentId=1&courseId=1&videoId=1" "查询视频进度"
test_get "/api/progress/video/list?studentId=1&courseId=1" "查询视频进度列表"
test_get "/api/progress/document?studentId=1&courseId=1&documentId=1" "查询文档进度"
test_get "/api/progress/document/list?studentId=1&courseId=1" "查询文档进度列表"
test_get "/api/progress/course/all?studentId=1&courseId=1" "查询全部进度"

# =====================================================
# 7. 课程视频控制器 (CourseVideoController)
# =====================================================
print_header "7. 课程视频控制器 (CourseVideoController)"

test_get "/api/course/video/list?courseId=1" "按课程列出视频"

# =====================================================
# 8. 课程文档控制器 (CourseDocumentController)
# =====================================================
print_header "8. 课程文档控制器 (CourseDocumentController)"

test_get "/api/course/document/list?courseId=1" "按课程列出文档"

# =====================================================
# 9. 作业提交控制器 (SubmissionController)
# =====================================================
print_header "9. 作业提交控制器 (SubmissionController)"

test_get "/api/submission" "查看所有作业提交"
test_get "/api/submission/1" "按作业ID查询提交"
test_get "/api/submission/detail/1" "获取提交详情"
test_get "/api/submission/1/comment" "获取小组评语"
test_get "/api/submission/my-group?groupId=1" "查询小组提交记录"

# =====================================================
# 10. 教师作业控制器 (TeacherAssignmentController)
# =====================================================
print_header "10. 教师作业控制器 (TeacherAssignmentController)"

test_get "/api/teacherAssignments/1" "按教师ID查询作业"
test_get "/api/teacherAssignments/by-course/1" "按课程查询作业"
test_get "/api/teacherAssignments" "查询所有作业"

# =====================================================
# 11. 教师评分控制器 (TeacherGradingController)
# =====================================================
print_header "11. 教师评分控制器 (TeacherGradingController)"

test_get "/api/grading/group/1" "获取小组评分"

# =====================================================
# 12. AI考试控制器 (AiExamController)
# =====================================================
print_header "12. AI考试控制器 (AiExamController)"

test_get "/api/aiexam/accuracy?studentId=1&courseId=1" "查询题目正确率"
test_get "/api/aiexam/average-score?studentId=1&courseId=1" "查询平均成绩"
test_get "/api/aiexam/detailed-scores?studentId=1&courseId=1" "查询详细成绩"
test_get "/api/aiexam/list?studentId=1&courseId=1" "查询考试记录"

# =====================================================
# 13. 会话控制器 (ConversationController)
# =====================================================
print_header "13. 会话控制器 (ConversationController)"

test_get "/api/ai/conversation/list/testuser" "获取用户会话列表"
test_get "/api/ai/conversation/1/messages" "获取会话消息"

# =====================================================
# 14. 学生小组控制器 (StudentGroupController)
# =====================================================
print_header "14. 学生小组控制器 (StudentGroupController)"

test_get "/api/student-group" "查询所有小组"

# =====================================================
# 15. 小组成员控制器 (GroupMemberController)
# =====================================================
print_header "15. 小组成员控制器 (GroupMemberController)"

# POST请求测试需要真实数据

# =====================================================
# 16. 文件上传控制器 (UploadController)
# =====================================================
print_header "16. 文件上传控制器 (UploadController)"

# 文件上传需要真实文件，跳过

# =====================================================
# 17. 分片上传控制器 (ChunkUploadController)
# =====================================================
print_header "17. 分片上传控制器 (ChunkUploadController)"

# 分片上传需要初始化，跳过

# =====================================================
# 测试总结
# =====================================================
print_header "测试总结"
echo "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -gt 0 ]; then
    echo -e "\n${RED}失败的API列表:${NC}"
    for api in "${FAILED_APIS[@]}"; do
        echo -e "  ${RED}✗${NC} $api"
    done
fi

echo -e "\n测试结束时间: $(date '+%Y-%m-%d %H:%M:%S')"

# 退出码
if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi