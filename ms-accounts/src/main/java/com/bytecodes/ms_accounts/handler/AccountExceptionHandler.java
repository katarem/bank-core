package com.bytecodes.ms_accounts.handler;

import com.bytecodes.ms_accounts.handler.dto.ErrorDetails;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.DailyWithdrawalLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.InsufficientBalanceException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.handler.exceptions.UserNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Arrays;

/**
 * Clase que manejará las excepciones globales
 */
@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(value = {
            CreateAccountLimitExceededException.class,
            CustomerIsInactiveException.class,
            InsufficientBalanceException.class,
            DailyWithdrawalLimitExceededException.class
    })
    public ResponseEntity<ErrorDetails> handleBusinessExceptions(RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDetails.builder()
                        .code("BUSINESS_RULE_VIOLATION")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorDetails> handleFeignException(feign.FeignException ex) {

        var status = ex.status() > 0 ? HttpStatusCode.valueOf(ex.status()) : HttpStatus.BAD_GATEWAY;//En pruebas se identificó que sí está abajo el ms-customer el status devuelto es -1 con el mensaje ConnectionRefused
        String message = "Ha ocurrido un error interno de comunicación. Intente más tarde. Sí el problema persiste contacte al administrador";

        ErrorDetails error = ErrorDetails.builder()
                .code("CUSTOMER_SERVICE_ERROR")
                .message(message)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Maneja la excepción {@link HttpMessageNotReadableException} que ocurre
     * cuando el cuerpo de la petición no puede ser deserializado correctamente.
     *
     * <p>
     * Esta implementación personaliza el mensaje de error cuando la causa
     * corresponde a un valor inválido para un tipo {@code enum}, indicando
     * explícitamente el valor recibido y los valores permitidos.
     * </p>
     *
     * @param ex excepción lanzada cuando el mensaje HTTP no puede ser leído
     *           o convertido al tipo esperado (por ejemplo, JSON mal formado
     *           o valor inválido para un enum).
     *
     *
     * @return {@link ResponseEntity} con código {@code 400 BAD_REQUEST}
     *         y un objeto {@link ErrorDetails} que contiene el código de error,
     *         el mensaje descriptivo y la marca de tiempo.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetails> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
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

    @ExceptionHandler(NotOwnAccountException.class)
    public ResponseEntity<ErrorDetails> notOwnAccount(NotOwnAccountException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorDetails.builder()
                        .code("ACCOUNT_ACCESS_NOT_GRANTED")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorDetails> accountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorDetails.builder()
                        .code("ACCOUNT_NOT_FOUND")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDetails> userNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorDetails.builder()
                        .code("CUSTOMER_NOT_FOUND")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }

}
