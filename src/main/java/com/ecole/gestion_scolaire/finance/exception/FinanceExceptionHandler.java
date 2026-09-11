package com.ecole.gestion_scolaire.finance.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class FinanceExceptionHandler {
    @ExceptionHandler(FinanceNotFoundException.class)
    ResponseEntity<?> notFound(FinanceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(e));
    }

    @ExceptionHandler(FinanceBusinessException.class)
    ResponseEntity<?> business(FinanceBusinessException e) {
        return ResponseEntity.badRequest().body(body(e));
    }

    private Map<String, Object> body(Exception e) {
        return Map.of("timestamp", OffsetDateTime.now().toString(), "message", e.getMessage());
    }
}
