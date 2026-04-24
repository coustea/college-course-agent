#!/bin/bash

# ========================================
# CCUT 服务管理脚本
# 用法: ./deploy.sh {start|stop|restart|status|logs|build}
# ====================

set -e

# ==================== 配置区 ====================
APP_NAME="ccut"
APP_DIR="/home/ubuntu/ccut"
JAR_NAME="service-0.0.1-SNAPSHOT.jar"
LOG_FILE="$APP_DIR/app.log"
PID_FILE="$APP_DIR/app.pid"

# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="ccut"
DB_USER="coustea"
DB_PASSWORD="Aa123456"

# Redis配置
REDIS_HOST="localhost"
REDIS_PORT="6379"
REDIS_PASSWORD="123456"

# 上传文件路径
UPLOAD_DIR="/home/ubuntu/ccut/uploads"

# JVM配置
JVM_OPTS="-Xms512m -Xmx1024m"
# ==================== 配置区结束 ====================

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_blue() {
    echo -e "${BLUE}$1${NC}"
}

# 构建Java命令
build_java_cmd() {
    local cmd="java $JVM_OPTS"
    cmd="$cmd -Dspring.data.redis.host=$REDIS_HOST"
    cmd="$cmd -Dspring.data.redis.port=$REDIS_PORT"
    [ -n "$REDIS_PASSWORD" ] && cmd="$cmd -Dspring.data.redis.password=$REDIS_PASSWORD"
    cmd="$cmd -Dspring.datasource.url=jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true"
    cmd="$cmd -Dspring.datasource.username=$DB_USER"
    cmd="$cmd -Dspring.datasource.password=$DB_PASSWORD"
    cmd="$cmd -Dfile.upload-dir=$UPLOAD_DIR"
    cmd="$cmd -jar $APP_DIR/$JAR_NAME"
    echo "$cmd"
}

# 检查是否运行
is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# 获取PID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

# 启动服务
start() {
    if is_running; then
        log_warn "服务已在运行中 (PID: $(get_pid))"
        return 1
    fi

    log_info "启动 $APP_NAME 服务..."

    # 检查JAR文件
    if [ ! -f "$APP_DIR/$JAR_NAME" ]; then
        log_error "找不到JAR文件: $APP_DIR/$JAR_NAME"
        return 1
    fi

    # 创建上传目录
    mkdir -p "$UPLOAD_DIR"

    # 启动服务
    cd "$APP_DIR"
    nohup $(build_java_cmd) > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"

    sleep 3

    if is_running; then
        log_info "服务启动成功 (PID: $(get_pid))"
        log_info "日志文件: $LOG_FILE"
    else
        log_error "服务启动失败，请查看日志: $LOG_FILE"
        return 1
    fi
}

# 停止服务
stop() {
    if ! is_running; then
        log_warn "服务未运行"
        [ -f "$PID_FILE" ] && rm -f "$PID_FILE"
        return 0
    fi

    local pid=$(get_pid)
    log_info "停止 $APP_NAME 服务 (PID: $pid)..."

    kill "$pid" 2>/dev/null

    # 等待进程结束
    local count=0
    while ps -p "$pid" > /dev/null 2>&1 && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
    done

    if ps -p "$pid" > /dev/null 2>&1; then
        log_warn "进程未响应，强制终止..."
        kill -9 "$pid" 2>/dev/null
    fi

    rm -f "$PID_FILE"
    log_info "服务已停止"
}

# 重启服务
restart() {
    log_info "重启 $APP_NAME 服务..."
    stop
    sleep 2
    start
}

# 查看状态
status() {
    if is_running; then
        log_info "服务运行中 (PID: $(get_pid))"
        log_blue "----------------------------------------"
        log_blue "应用目录: $APP_DIR"
        log_blue "日志文件: $LOG_FILE"
        log_blue "数据库:   $DB_HOST:$DB_PORT/$DB_NAME"
        log_blue "Redis:    $REDIS_HOST:$REDIS_PORT"
        log_blue "上传目录: $UPLOAD_DIR"
        log_blue "----------------------------------------"
        # 显示内存使用
        local pid=$(get_pid)
        local mem=$(ps -o rss= -p "$pid" 2>/dev/null | awk '{printf "%.1f MB", $1/1024}')
        log_blue "内存使用: $mem"
    else
        log_warn "服务未运行"
        [ -f "$PID_FILE" ] && log_warn "PID文件存在但进程已退出" && rm -f "$PID_FILE"
    fi
}

# 查看日志
logs() {
    if [ ! -f "$LOG_FILE" ]; then
        log_warn "日志文件不存在: $LOG_FILE"
        return 1
    fi

    local lines=${1:-100}
    log_info "显示最近 $lines 行日志:"
    echo ""
    tail -n "$lines" "$LOG_FILE"
}

# 实时日志
logs_follow() {
    if [ ! -f "$LOG_FILE" ]; then
        log_warn "日志文件不存在: $LOG_FILE"
        return 1
    fi

    log_info "实时日志 (Ctrl+C 退出):"
    tail -f "$LOG_FILE"
}

# 帮助信息
help() {
    echo ""
    log_blue "用法: $0 {start|stop|restart|status|logs|logs-follow}"
    echo ""
    echo "命令说明:"
    echo "  start       启动服务"
    echo "  stop        停止服务"
    echo "  restart     重启服务"
    echo "  status      查看服务状态"
    echo "  logs        查看最近日志 (默认100行)"
    echo "  logs-follow 实时查看日志"
    echo ""
}

# 主入口
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs ${2:-100}
        ;;
    logs-follow)
        logs_follow
        ;;
    *)
        help
        exit 1
        ;;
esac
