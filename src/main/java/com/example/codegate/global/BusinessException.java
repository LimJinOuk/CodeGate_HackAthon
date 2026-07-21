package com.example.codegate.global;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public BusinessException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
