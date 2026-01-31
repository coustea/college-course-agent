package com.ccut.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "请求成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    INVALID_PASSWORD(1003, "密码错误"),
    INVALID_TOKEN(1004, "Token无效或已过期"),

    STUDENT_NOT_FOUND(2001, "学生不存在"),
    TEACHER_NOT_FOUND(2002, "教师不存在"),
    COURSE_NOT_FOUND(2003, "课程不存在"),
    CONVERSATION_NOT_FOUND(2004, "会话不存在"),
    EXAM_NOT_FOUND(2005, "考试不存在"),

    CONVERSATION_LIMIT_EXCEEDED(3001, "当前会话已达到最大对话轮数，请创建新会话"),
    MESSAGE_EMPTY(3002, "消息内容不能为空"),
    FILE_UPLOAD_FAILED(3003, "文件上传失败"),
    AI_CALL_FAILED(3004, "AI调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}