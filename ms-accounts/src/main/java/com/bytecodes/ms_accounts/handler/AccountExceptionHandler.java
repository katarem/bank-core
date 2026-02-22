package com.bytecodes.ms_accounts.handler;

import com.bytecodes.ms_accounts.handler.dto.ErrorDetails;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clase que manejará las excepciones globales
 */
@RestControllerAdvice
public class AccountExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorDetails> handleFeignException(feign.FeignException ex) {

        HttpStatus status = HttpStatus.BAD_GATEWAY;
        String message = "Error comunicándose con el microservicio customer";

        if (ex.status() == 401) {//Sucede cuando el microservicio no existe o el token no es válido
            status = HttpStatus.UNAUTHORIZED;
            message = "No autorizado para acceder al microservicio customer";
        } else if (ex.status() == 404) {
            status = HttpStatus.NOT_FOUND;
            message = "Cliente no encontrado en el microservicio customer";
        } else if (ex.status() == 500) {
            status = HttpStatus.BAD_GATEWAY;
            message = "Error interno en el microservicio customer";
        }

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
     * @param headers cabeceras HTTP de la solicitud que generó la excepción.
     *
     * @param status código de estado HTTP que Spring propone para la respuesta.
     *
     * @param request contexto de la petición web actual que contiene
     *                información adicional sobre la solicitud.
     *
     * @return {@link ResponseEntity} con código {@code 400 BAD_REQUEST}
     *         y un objeto {@link ErrorDetails} que contiene el código de error,
     *         el mensaje descriptivo y la marca de tiempo.
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

        //De la excepción obtenemos la lista de errores
        Map<String, String> mapErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                error -> {
            String errorMessage = error.getDefaultMessage();
            return errorMessage != null ? errorMessage : "";
        }));

        //return super.handleMethodArgumentNotValid(ex, headers, status, request);
        return ResponseEntity.status(status).body(mapErrors);
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

}
