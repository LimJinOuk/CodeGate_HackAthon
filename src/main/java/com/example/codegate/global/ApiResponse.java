package com.example.codegate.global;

import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    public static ApiResponse<Void> error(String code, String message, Map<String, Object> details) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message, details));
    }

    public record ErrorBody(String code, String message, Map<String, Object> details) {

        public ErrorBody(String code, String message) {
            this(code, message, Map.of());
        }
    }
}
