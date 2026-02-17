package com.bankaccountmanagement.bank_account_management.Execptions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        logger.log(Level.WARNING, "Entity not found: {0}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.log(Level.WARNING, "Illegal argument: {0}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        logger.log(Level.WARNING, "Bad credentials attempt");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", 
            "Invalid username or password");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        logger.log(Level.WARNING, "Username not found");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", 
            "Invalid username or password");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("validationErrors", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        logger.log(Level.WARNING, "Insufficient funds: {0}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Transaction failed", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.log(Level.SEVERE, "Unexpected error: {0}", ex.getMessage());
        // Don't expose internal error details to client
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", 
            "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

}
