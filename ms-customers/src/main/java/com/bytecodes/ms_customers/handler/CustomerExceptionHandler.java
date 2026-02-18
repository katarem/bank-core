package com.bytecodes.ms_customers.handler;

import java.time.Instant;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomerExceptionHandler {

    @ExceptionHandler(value = {DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, String>> duplicatedData(DataIntegrityViolationException ex) {
        
        String code = "CUSTOMER_CONFLICT";
        StringBuilder messageBuilder = new StringBuilder();

        if (ex.getMostSpecificCause().getMessage().contains("(dni)")) {
            messageBuilder.append("El DNI ya está registrado").append(",");
        }
        if (ex.getMostSpecificCause().getMessage().contains("(email)")) {
            messageBuilder.append("El email ya está registrado").append(",");
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code", code,
                        "message", messageBuilder.toString(),
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validationError(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        for (ObjectError error : ex.getAllErrors()) {
            sb.append(error.getDefaultMessage()).append(",");
        }
        String errors = sb.toString();
        String timestamp = Instant.now().toString();
        String code = "INVALID_FIELDS";

        return ResponseEntity
            .badRequest()
            .body(Map.of(
                "code", code, 
                "errors", errors, 
                "timestamp", timestamp
            ));

    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> badCredentialsError() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

}
