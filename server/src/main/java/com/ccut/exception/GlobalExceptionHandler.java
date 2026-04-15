package com.ccut.exception;

import com.ccut.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，记录详细日志并返回友好的错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     * 记录业务异常详情，包括请求 URI、用户信息等
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        log.warn("========== 业务异常 ==========");
        log.warn("请求信息：{} {} from {}", method, uri, remoteAddr);
        log.warn("异常类型：{}", e.getClass().getSimpleName());
        log.warn("错误码：{}, 消息：{}", e.getCode(), e.getMessage());
        log.debug("User-Agent: {}", userAgent);
        log.debug("异常堆栈:", e);
        
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理（@Valid）
     * 记录参数校验失败的详细信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("========== 参数校验失败 ==========");
        log.warn("请求 URI: {}", uri);
        log.warn("校验错误：{}", errorMsg);
        log.debug("错误详情:", e);
        
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), errorMsg);
    }

    /**
     * 参数绑定异常处理
     * 记录参数绑定失败的详细信息
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("========== 参数绑定失败 ==========");
        log.warn("请求 URI: {}", uri);
        log.warn("绑定错误：{}", errorMsg);
        log.debug("错误详情:", e);
        
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), errorMsg);
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.warn("========== 非法参数异常 ==========");
        log.warn("请求：{} {}", method, uri);
        log.warn("错误消息：{}", e.getMessage());
        log.debug("异常堆栈:", e);
        
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 空指针异常处理
     * 记录空指针异常的详细上下文信息，便于排查问题
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        
        log.error("========== 空指针异常 (严重) ==========");
        log.error("请求信息：{} {} from {}", method, uri, remoteAddr);
        log.error("异常消息：{}", e.getMessage());
        log.error("异常堆栈:", e);
        
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
    }


    /**
     * 其他未捕获异常处理
     * 作为最后一道防线，捕获所有未处理的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String queryString = request.getQueryString();
        
        log.error("========== 未捕获异常 (严重) ==========");
        log.error("请求信息：{} {} from {}", method, uri, remoteAddr);
        log.error("请求参数：{}", queryString);
        log.error("User-Agent: {}", userAgent);
        log.error("异常类型：{}", e.getClass().getName());
        log.error("异常消息：{}", e.getMessage());
        log.error("完整堆栈:", e);

        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
    }
}
