package com.aviscribe.common.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[BAD_REQUEST] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("[CONFLICT] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 409);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleValidationException(Exception ex) {
        log.warn("[VALIDATION_ERROR] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 422);
        body.put("message", "请求参数不合法");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("[BAD_REQUEST] 请求体不是合法 JSON");
        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("message", "请求体格式错误");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[FORBIDDEN] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 403);
        body.put("message", "无权访问该资源");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("[UNAUTHORIZED] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("[SERVER_ERROR]", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 500);
        body.put("message", "服务内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

