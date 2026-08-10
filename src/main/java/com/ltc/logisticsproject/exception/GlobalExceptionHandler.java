package com.ltc.logisticsproject.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Bütün controller-lərdə hər yerdə "throw new RuntimeException("...")" ilə
// atılan (əsasən Azərbaycanca, istifadəçiyə göstərilməli) mesajlar üçün
// mərkəzi tutucu. Bundan əvvəl bu istisnalar Spring Boot-un defolt
// error handler-inə düşürdü (server.error.include-message=always
// sayəsində eyni {timestamp,status,error,message,path} formasını
// qaytarırdı) — burada həmin format QƏSDƏN eynilə saxlanılıb ki, frontend-in
// err.response.data.message (21 fayl, 41 istifadə) və Android-in
// ApiErrorUtils.kt-ın gözlədiyi "message" sahəsi dəyişməsin. Yəni bu, davranışı
// dəyişmir — sadəcə onu implisit defolt handler-dən çıxarıb bura, aydın və
// test edilə bilən bir yerə köçürür, üstəlik stack trace-i logda saxlayır
// (əvvəllər bunu konteyner özü edirdi, indi handler tutduğu üçün özümüz
// log etməliyik ki, görünürlük itməsin).
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Gözlənilməyən xəta: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    // Spring Security-nin @PreAuthorize/hasRole yoxlamalarından atılan
    // AccessDeniedException adətən Security-nin öz filter zəncirində tutulur,
    // amma controller metodunun İÇİNDƏ (filter-dən sonra) atılarsa bura düşür —
    // eyni cavab formasını saxlamaq üçün ayrıca işlənir.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("İcazə rədd edildi: {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, "Bu əməliyyat üçün icazəniz yoxdur", request);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
