package com.example.projectCollab.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // EMAIL EXISTS
    // ==========================================

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailExists(
            EmailAlreadyExistsException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // ==========================================
    // BAD LOGIN
    // ==========================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", "Invalid email or password");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(PendingVerificationException.class)
    public ResponseEntity<Map<String, String>> handlePendingVerification(
            PendingVerificationException ex
    ) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "This account is not active. Contact an administrator.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(LockedException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "This account has been suspended. Contact an administrator.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ==========================================
    // VALIDATION
    // ==========================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        if (!errors.containsKey("error") && !errors.isEmpty()) {
            errors.put("error", errors.values().iterator().next());
        }

        return ResponseEntity
                .badRequest()
                .body(errors);
    }

    // ==========================================
    // ILLEGAL ARGUMENT
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    // ==========================================
    // UNAUTHORIZED ACCESS
    // ==========================================

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedAccess(
            UnauthorizedAccessException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    // ==========================================
    // ILLEGAL STATE
    // ==========================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(
            IllegalStateException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    // ==========================================
    // RESOURCE NOT FOUND
    // ==========================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(
            ResourceNotFoundException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, String>> handleFileStorage(FileStorageException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Map<String, String> response = new HashMap<>();
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String lower = message == null ? "" : message.toLowerCase();
        if (lower.contains("student_id")) {
            response.put("error", "This student ID is already registered. Lecturers can leave student ID blank.");
        } else if (lower.contains("duplicate") && lower.contains("email")) {
            response.put("error", "Email is already registered.");
        } else if (lower.contains("duplicate") && lower.contains("username")) {
            response.put("error", "Username is already taken.");
        } else if (lower.contains("comment_id")) {
            response.put("error", "Restart the backend so the file library schema can update, then try uploading again.");
        } else {
            response.put("error", "Could not save this record. Please check your details and try again.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==========================================
    // GENERIC EXCEPTION (Optional - Catch all)
    // ==========================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(
            Exception ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("error", "An unexpected error occurred: " + ex.getMessage());

        // Log the error for debugging
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}