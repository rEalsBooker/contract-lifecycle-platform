package com.contractguard.platform.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handle(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of(
                "code", exception.getCode(),
                "message", exception.getMessage(),
                "path", request.getRequestURI(),
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}

