package com.bytecodes.ms_accounts.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
public class CreateTransferRequest {
    @NotNull
    private UUID sourceAccountId;
    @NotNull
    @NotBlank
    @Pattern(regexp = "^ES\\d{22}$", message = "El IBAN debe seguir el formato español")
    private String destinationAccountNumber;
    @DecimalMin(value = "1.0", message = "La cantidad mínima para transferencia es de 1 euro")
    @DecimalMax(value = "10000", message = "la cantidad máxima para transferencia es de 10000 euros")
    private BigDecimal amount;
    private String concept;
    private Date scheduledDate;
}
