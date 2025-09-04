package com.ccut.common.exception;

import com.ccut.common.entity.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerException.class)
    public ApiResult<?> handleCustomerException(CustomerException ex) {
        String code = ex.getCode() == null ? "500" : ex.getCode();
        String message = ex.getMsg() == null ? ex.getMessage() : ex.getMsg();
        int status = 500;
        try { status = Integer.parseInt(code); } catch (Exception ignored) {}
        return new ApiResult<>(status, message, null);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<?> handleOther(Exception ex) {
        return new ApiResult<>(500, ex.getMessage(), null);
    }
}


