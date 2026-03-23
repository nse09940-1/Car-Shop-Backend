package main.presentation;

import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.exception.IncompatibleComponentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(Instant.now(), exception.getMessage()));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiError> handleValidation(DomainValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(Instant.now(), exception.getMessage()));
    }

    @ExceptionHandler(IncompatibleComponentException.class)
    public ResponseEntity<ApiError> handleIncompatible(IncompatibleComponentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(Instant.now(), "Internal server error"));
    }

    public record ApiError(Instant timestamp, String message) {
    }
}

