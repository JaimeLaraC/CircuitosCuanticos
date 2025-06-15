package com.example.beusuarios.exception;

import com.example.beusuarios.dto.ErrorResponseDto; // Adjust package if ErrorResponseDto is elsewhere
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import jakarta.persistence.EntityNotFoundException; // Or your custom not found exception

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> details = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.toList());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed for one or more fields.",
                request.getDescription(false).replace("uri=", ""),
                details
        );
        logger.warn("Validation failed for request {}: {}", request.getDescription(false), details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {

        // Customize message based on specific constraint violation if possible/needed
        // For now, a generic message.
        String message = "Database error: A data integrity rule was violated.";
        if (ex.getCause() != null && ex.getCause().getMessage().contains("UK_")) { // Example check for unique constraint
            message = "Database error: A unique constraint was violated (e.g., email already exists).";
        }

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(), // 409 Conflict is often suitable
                "Data Integrity Violation",
                message,
                request.getDescription(false).replace("uri=", "")
        );
        logger.error("Data integrity violation for request {}: {}", request.getDescription(false), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class) // Or a custom UserNotFoundException
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(
            EntityNotFoundException ex, WebRequest request) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(), // Or a generic "The requested resource was not found."
                request.getDescription(false).replace("uri=", "")
        );
        logger.warn("Entity not found for request {}: {}", request.getDescription(false), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Example: Custom exception for insufficient credit
    @ExceptionHandler(InsufficientCreditException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientCredit(
        InsufficientCreditException ex, WebRequest request) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(), // 400 or 402 Payment Required could also be considered
                "Insufficient Credit",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        logger.warn("Insufficient credit for request {}: {}", request.getDescription(false), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class) // Generic fallback handler
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception ex, WebRequest request) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.", // Don't expose ex.getMessage() directly for generic exceptions
                request.getDescription(false).replace("uri=", "")
        );
        logger.error("Unhandled exception for request {}:", request.getDescription(false), ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
