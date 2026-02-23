package com.bytecodes.ms_accounts.response;

import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummary {
    private UUID id;
    private String accountNumber;
    private AccountType accountType;
    private String currency;
    private BigDecimal balance;
    private String alias;
    private AccountStatus status;
}
