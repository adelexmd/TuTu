package com.tutu.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：将未捕获异常统一为 Result 结构，避免破坏前端约定的响应格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e) {
        return Result.error("AI 服务响应超时或异常，请重试");
    }
}
