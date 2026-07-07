package com.tutu.common;

/**
 * 统一 API 响应结构：{ "code": 0, "data": {...}, "message": "success" }
 */
public class Result<T> {
    private int code;
    private T data;
    private String message;

    public Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, data, "success");
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(-1, null, message);
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
