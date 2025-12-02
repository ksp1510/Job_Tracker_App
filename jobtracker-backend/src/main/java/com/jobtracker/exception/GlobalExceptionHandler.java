package com.jobtracker.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * SECURITY: Global exception handler to prevent information disclosure
 * Returns generic error messages in production
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private Map<String, Object> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        return error;
    }

    // Handles custom errors like duplicate email
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        logger.warn("Duplicate email attempt: {}", ex.getMessage());
        return new ResponseEntity<>(
            createErrorResponse("Email already registered", HttpStatus.BAD_REQUEST),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleMongoDuplicate(DuplicateKeyException ex) {
        logger.warn("Duplicate key error: {}", ex.getMessage());
        return new ResponseEntity<>(
            createErrorResponse("A record with this information already exists", HttpStatus.BAD_REQUEST),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        logger.debug("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(
            createErrorResponse("Resource not found", HttpStatus.NOT_FOUND),
            HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Invalid input: {}", ex.getMessage());
        // Show specific validation message as it doesn't expose internal details
        return new ResponseEntity<>(
            createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(
            createErrorResponse("Access denied", HttpStatus.FORBIDDEN),
            HttpStatus.FORBIDDEN
        );
    }

    // Handles @Valid validation errors (missing fields, invalid email, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // ✅ ADD LOGGING
        System.err.println("❌ Validation errors: " + errors);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.badRequest().body(response);
    }

    // SECURITY FIX: Handles any other unexpected exception without exposing internals
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        // Log full exception details for debugging
        logger.error("Unexpected error occurred", ex);

        String message;
        if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
            // SECURITY: Generic message in production
            message = "An unexpected error occurred. Please try again later.";
        } else {
            // More details in development for debugging
            message = "Error: " + ex.getClass().getSimpleName();
        }

        return new ResponseEntity<>(
            createErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime error: ", ex);

        String message;
        if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
            // SECURITY: Generic message in production
            message = "An error occurred while processing your request";
        } else {
            // More details in development
            message = ex.getMessage() != null ? ex.getMessage() : "Runtime error occurred";
        }

        return new ResponseEntity<>(
            createErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
