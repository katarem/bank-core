package com.bytecodes.ms_customers.handler;

import java.time.Instant;
import java.util.Map;

import com.bytecodes.ms_customers.util.ErrorDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomerExceptionHandler {

    @ExceptionHandler(value = {DataIntegrityViolationException.class})
    public ResponseEntity<ErrorDetails> duplicatedData(DataIntegrityViolationException ex) {
        
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
                .body(ErrorDetails.builder()
                        .code(code)
                        .message(messageBuilder.toString())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> validationError(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder();
        for (ObjectError error : ex.getAllErrors()) {
            errors.append(error.getDefaultMessage()).append(",");
        }

        return ResponseEntity
            .badRequest()
            .body(ErrorDetails.builder()
                    .code("INVALID_FIELDS")
                    .message(errors.toString())
                    .timestamp(Instant.now())
                    .build());

    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorDetails> userNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDetails.builder()
                .code("CUSTOMER_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> badCredentialsError() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> invalidUuidFormat(MethodArgumentTypeMismatchException ex) {
        var response = Map.of(
                "error", "INVALID_UUID_FORMAT",
                "message", "Formato de ID no válido. Introduce una ID válida",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}
