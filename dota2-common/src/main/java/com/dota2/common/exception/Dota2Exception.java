package com.dota2.common.exception;

public class Dota2Exception extends RuntimeException {

    private String code;

    public Dota2Exception(String message) {
        super(message);
        this.code = "999999";
    }

    public Dota2Exception(String code, String message) {
        super(message);
        this.code = code;
    }

    public Dota2Exception(String message, Throwable cause) {
        super(message, cause);
        this.code = "999999";
    }

    public String getCode() {
        return code;
    }
}
