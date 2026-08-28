package com.pricehunter.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
/** Преобразует исключения приложения в единый и безопасный формат HTTP-ошибок. */
public class GlobalExceptionHandler {

    /** Возвращает 400 и карту ошибок полей при нарушении ограничений DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", fields);
    }

    /** Возвращает 400, если JSON отсутствует или не может быть прочитан. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableMessage() {
        return response(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", Map.of());
    }

    /** Возвращает 409 для явно обнаруженного бизнес-конфликта. */
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    /** Преобразует конфликт ограничения базы в нейтральный ответ 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrityViolation() {
        return response(HttpStatus.CONFLICT, "The request conflicts with existing data", Map.of());
    }

    /** Собирает тело и HTTP-статус ответа об ошибке. */
    private ResponseEntity<ApiError> response(HttpStatus status, String message, Map<String, String> fields) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fields);
        return ResponseEntity.status(status).body(body);
    }
}
