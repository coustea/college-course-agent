package com.ccut.common.Utils;

import com.ccut.common.entity.ApiResult;

public class ApiResultHandler {

    public static ApiResult success(Object object){
        ApiResult apiResult = new ApiResult();
        apiResult.setData(object);
        apiResult.setCode(200);
        apiResult.setMessage("请求成功");
        return apiResult;
    }

    public static <T> ApiResult success(String message,T data){
        ApiResult apiResult = new ApiResult<>();
        apiResult.setCode(200);
        apiResult.setMessage(message);
        apiResult.setData(data);
        return apiResult;
    }

    public static <T> ApiResult buildApiResult(Integer code,String message, T data){

        ApiResult apiResult = new ApiResult<>();

        apiResult.setData(data);
        apiResult.setCode(code);
        apiResult.setMessage(message);
        return apiResult;
    }
}
