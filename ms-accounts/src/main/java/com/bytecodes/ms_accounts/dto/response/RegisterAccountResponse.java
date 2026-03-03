package com.bytecodes.ms_accounts.dto.response;

import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
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
public class RegisterAccountResponse {
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
