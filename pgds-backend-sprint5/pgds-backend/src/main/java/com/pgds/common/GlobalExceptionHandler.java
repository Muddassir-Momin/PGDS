package com.pgds.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<Map<String, Object>> body(HttpStatus s, String msg, Object details) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", s.value());
        m.put("error", msg);
        if (details != null) m.put("details", details);
        return ResponseEntity.status(s).body(m);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return body(e.getStatus(), e.getMessage(), null); }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    ResponseEntity<?> auth(Exception e) { return body(HttpStatus.UNAUTHORIZED, "Invalid username or password", null); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> denied(AccessDeniedException e) { return body(HttpStatus.FORBIDDEN, "Access denied", null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }
}
