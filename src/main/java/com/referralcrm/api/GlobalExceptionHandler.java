package com.referralcrm.api;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private Map<String,Object> body(String message, HttpStatus status) { return Map.of("timestamp", OffsetDateTime.now(), "status", status.value(), "error", status.getReasonPhrase(), "message", message); }
    @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status()).body(body(e.getMessage(),e.status())); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().map(x -> x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Invalid request");
        return ResponseEntity.badRequest().body(body(msg,HttpStatus.BAD_REQUEST));
    }
    @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<?> constraint(ConstraintViolationException e) { return ResponseEntity.badRequest().body(body("Request parameters failed validation",HttpStatus.BAD_REQUEST)); }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class) ResponseEntity<?> typeMismatch(MethodArgumentTypeMismatchException e) { return ResponseEntity.badRequest().body(body("Invalid value for parameter "+e.getName(),HttpStatus.BAD_REQUEST)); }
    @ExceptionHandler(BadCredentialsException.class) ResponseEntity<?> credentials() { return ResponseEntity.status(401).body(body("Email or password is incorrect",HttpStatus.UNAUTHORIZED)); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(body("The requested change conflicts with existing data",HttpStatus.CONFLICT)); }
}
