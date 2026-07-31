package com.dota2.common.utils;

public class Result<T> {

    public static final String CODE_SUCCESS = "000000";
    public static final String CODE_FAIL = "999999";
    public static final String MSG_SUCCESS = "Success";
    public static final String MSG_FAIL = "Fail";

    private String code;
    private String message;
    private T data;

    public Result() {}

    public Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(T data) {
        this.code = CODE_SUCCESS;
        this.message = MSG_SUCCESS;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, MSG_SUCCESS, null);
    }

    public static <T> Result<T> fail() {
        return new Result<>(CODE_FAIL, MSG_FAIL, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(CODE_FAIL, message, null);
    }

    public static <T> Result<T> build(String code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> build(String code, String message, T data) {
        return new Result<>(code, message, data);
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
