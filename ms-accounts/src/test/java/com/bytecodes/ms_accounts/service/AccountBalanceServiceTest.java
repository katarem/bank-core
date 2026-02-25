package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
public class AccountBalanceServiceTest {

    @Mock
    private AccountRepository repositoryAccount;

    @InjectMocks
    private AccountBalanceService service;

    @Test
    void apply_deposit_ok() {
        // given
        UUID accountId = UUID.randomUUID();
        BigDecimal depositMount = new BigDecimal("50");

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setBalance(BigDecimal.valueOf(100));

        // when
        when(repositoryAccount.findById(accountId)).thenReturn(Optional.of(accountEntity));

        // then
        service.applyDeposit(accountId, depositMount);

        assertEquals(BigDecimal.valueOf(150), accountEntity.getBalance());
        verify(repositoryAccount, times(1)).findById(accountId);

    }

    @Test
    void apply_deposit_throw_exception_account_not_found() {
        // given
        UUID accountId = UUID.randomUUID();
        BigDecimal depositMount = new BigDecimal("50");

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setBalance(BigDecimal.valueOf(100));

        // when
        when(repositoryAccount.findById(accountId)).thenReturn(Optional.empty());

        // then
        assertThrows(AccountNotFoundException.class, () -> service.applyDeposit(accountId, depositMount));
    }
}