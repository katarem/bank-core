package com.bytecodes.ms_customers.handler;

import java.time.Instant;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomerExceptionHandler {

    @ExceptionHandler(value = {DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, String>> duplicatedData(DataIntegrityViolationException ex) {
        
        String code = "CUSTOMER_CONFLICT";
        String message = "Ya existe un cliente con características similares.";

        if (ex.getMostSpecificCause() != null &&
            ex.getMostSpecificCause().getMessage().contains("(dni)")) {
            message = "El DNI ya está registrado";
            code = "DUPLICATED_DNI";
        } else if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage().contains("(email)")) {
            message = "El email ya está registrado";
            code = "DUPLICATED_EMAIL";
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code", code,
                        "message", message,
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

}
