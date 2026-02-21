package com.bytecodes.ms_accounts.handler;

import com.bytecodes.ms_accounts.handler.dto.ErrorDetails;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase que manejará las excepciones globales
 */
@RestControllerAdvice
public class CustomerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {
            CreateAccountLimitExceededException.class,
            CustomerIsInactiveException.class
    })
    public ResponseEntity<ErrorDetails> handleBusinessExceptions(RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDetails.builder()
                        .code("BUSINESS_RULE_VIOLATION")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    /**
     * Excepción personalizada para los campos tipo enums
     * @param ex
     * @param headers
     * @param status
     * @param request
     * @return
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = "Solicitud mal formada";

        // Validamos si la causa es por Enum inválido
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {

            if (invalidFormatException.getTargetType().isEnum()) {

                String invalidValue = invalidFormatException.getValue().toString();
                Object[] enumValues = invalidFormatException.getTargetType().getEnumConstants();

                message = "Valor inválido '" + invalidValue +
                        "'. Valores permitidos: " + Arrays.toString(enumValues);
            }
        }

        ErrorDetails error = ErrorDetails.builder()
                .code("INVALID_REQUEST")
                .message(message)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> mapErrors = new HashMap<>();

        //De la excepción obtenemos la lista de errores
        ex.getBindingResult().getFieldErrors().forEach(e -> {
            //para cada error encontrado, lo agregamos en nuestro mapa. El campo y el mensaje
            mapErrors.put(e.getField(), e.getDefaultMessage());
        });

        //return super.handleMethodArgumentNotValid(ex, headers, status, request);
        return ResponseEntity.status(status).body(mapErrors);
    }

}
