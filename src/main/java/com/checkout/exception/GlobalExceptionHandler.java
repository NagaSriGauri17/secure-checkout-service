package com.checkout.exception;

import com.checkout.dto.CheckoutDtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErrorResponse> handleCheckoutException(CheckoutException ex) {
        log.warn("Business error [{}]: {}", ex.getCode(), ex.getMessage());
        HttpStatus status = switch (ex.getCode()) {
            case "ORDER_NOT_FOUND"    -> HttpStatus.NOT_FOUND;
            case "ORDER_NOT_PAYABLE"  -> HttpStatus.CONFLICT;
            case "INVALID_TRANSITION" -> HttpStatus.CONFLICT;
            default                   -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(
            ErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message(details)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleStateError(IllegalStateException ex) {
        log.warn("State machine violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.builder()
                .code("INVALID_TRANSITION")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(
            ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again.")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }
}
