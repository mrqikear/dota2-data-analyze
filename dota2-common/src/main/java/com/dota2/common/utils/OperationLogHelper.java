package com.dota2.common.utils;

public class OperationLogHelper {

    private static final ThreadLocal<String> OPERATION_DETAIL = new ThreadLocal<>();

    public static void setDetail(String detail) {
        OPERATION_DETAIL.set(detail);
    }

    public static String getDetail() {
        return OPERATION_DETAIL.get();
    }

    public static void remove() {
        OPERATION_DETAIL.remove();
    }
}
