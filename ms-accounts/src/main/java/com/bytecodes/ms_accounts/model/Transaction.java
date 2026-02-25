package com.bytecodes.ms_accounts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private UUID id;
    private UUID accountId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String concept;
    private String counterpartyAccountNumber;
    private String counterpartyName;
    private String referenceNumber;
    private TransactionStatus status;
    private Instant createdAt;
}
