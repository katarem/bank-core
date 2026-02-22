package com.bytecodes.ms_accounts.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class Account {
    private UUID id;
    private String accountNumber;
    private UUID customerId;
    private AccountType accountType;
    private String currency;
    private BigDecimal balance;
    private String alias;
    private AccountStatus status;
    private BigDecimal dailyWithdrawalLimit;
    private Instant createdAt;
    private Instant updatedAt;
}
