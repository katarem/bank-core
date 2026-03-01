package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.DailyWithdrawalLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.InsufficientBalanceException;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.model.TransactionStatus;
import com.bytecodes.ms_accounts.model.TransactionType;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.repository.TransactionRepository;
import com.bytecodes.ms_accounts.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
public class AccountBalanceServiceTest {

    @Mock
    private AccountRepository repositoryAccount;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TransactionRepository repositoryTransaction;

    @InjectMocks
    private AccountBalanceService service;

    @Test
    void deposit_ok() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String token = "token";

        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal amount = new BigDecimal("25.00");

        DepositRequest request = DepositRequest.builder()
                .amount(amount)
                .description("Ingreso nomina")
                .build();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setCustomerId(customerId);
        accountEntity.setBalance(initialBalance);

        when(repositoryAccount.findById(accountId))
                .thenReturn(Optional.of(accountEntity));

        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID))
                .thenReturn(customerId.toString());

        when(repositoryTransaction.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity tx = invocation.getArgument(0);
                    if (tx.getId() == null) {
                        tx.setId(UUID.randomUUID());
                    }
                    if (tx.getCreatedAt() == null) {
                        tx.setCreatedAt(Instant.now());
                    }
                    return tx;
                });

        // when
        DepositResponse response = service.deposit(accountId, request, token);

        // then
        assertNotNull(response);
        assertEquals(TransactionType.DEPOSIT, response.getType());
        assertEquals(amount, response.getAmount());
        assertEquals(initialBalance, response.getBalanceBefore());
        assertEquals(new BigDecimal("125.00"), response.getBalanceAfter());

        assertEquals(new BigDecimal("125.00"), accountEntity.getBalance());

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(repositoryTransaction, times(2)).save(captor.capture());
        assertEquals(TransactionStatus.COMPLETED, captor.getValue().getStatus());
    }

    @Test
    void deposit_failed_when_apply_deposit_throws() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String token = "token";
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal amount = new BigDecimal("25.00");

        DepositRequest request = DepositRequest.builder()
                .amount(amount)
                .description("Ingreso nomina")
                .build();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setCustomerId(customerId);
        accountEntity.setBalance(initialBalance);

        when(repositoryAccount.findById(accountId))
                .thenReturn(Optional.of(accountEntity))   // primera llamada
                .thenThrow(new RuntimeException("db error")); // segunda llamada (applyDeposit)

        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID))
                .thenReturn(customerId.toString());

        when(repositoryTransaction.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity tx = invocation.getArgument(0);
                    if (tx.getId() == null) {
                        tx.setId(UUID.randomUUID());
                    }
                    if (tx.getCreatedAt() == null) {
                        tx.setCreatedAt(Instant.now());
                    }
                    return tx;
                });

        // when
        DepositResponse response = service.deposit(accountId, request, token);

        // then
        assertNotNull(response);
        assertEquals(initialBalance, response.getBalanceAfter());
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(repositoryTransaction, times(2)).save(captor.capture());
        assertEquals(TransactionStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void withdraw_ok() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String token = "token";

        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal amount = new BigDecimal("200.00");

        DepositRequest request = DepositRequest.builder()
                .amount(amount)
                .description("Retiro cajero automático")
                .build();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setCustomerId(customerId);
        accountEntity.setBalance(initialBalance);
        accountEntity.setDailyWithdrawalLimit(new BigDecimal("1000"));

        when(repositoryAccount.findById(accountId))
                .thenReturn(Optional.of(accountEntity));

        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID))
                .thenReturn(customerId.toString());

        LocalDate currentDateUtc = LocalDate.now(ZoneOffset.UTC);
        Instant start = currentDateUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = currentDateUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        when(repositoryTransaction.sumAmountByAccountAndTypeAndStatusBetween(
                eq(accountId),
                eq(TransactionType.WITHDRAW),
                eq(TransactionStatus.COMPLETED),
                eq(start),
                eq(end)
        )).thenReturn(BigDecimal.ZERO);

        when(repositoryTransaction.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity tx = invocation.getArgument(0);
                    if (tx.getId() == null) {
                        tx.setId(UUID.randomUUID());
                    }
                    if (tx.getCreatedAt() == null) {
                        tx.setCreatedAt(Instant.now());
                    }
                    return tx;
                });

        // when
        DepositResponse response = service.withdraw(accountId, request, token);

        // then
        assertNotNull(response);
        assertEquals(TransactionType.WITHDRAW, response.getType());
        assertEquals(amount, response.getAmount());
        assertEquals(initialBalance, response.getBalanceBefore());
        assertEquals(new BigDecimal("800.00"), response.getBalanceAfter());
        assertEquals(new BigDecimal("800.00"), accountEntity.getBalance());
    }

    @Test
    void withdraw_fails_when_insufficient_balance() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String token = "token";

        DepositRequest request = DepositRequest.builder()
                .amount(new BigDecimal("1200.00"))
                .description("Retiro cajero automático")
                .build();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setCustomerId(customerId);
        accountEntity.setBalance(new BigDecimal("1000.00"));
        accountEntity.setDailyWithdrawalLimit(new BigDecimal("1000"));

        when(repositoryAccount.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());

        assertThrows(InsufficientBalanceException.class, () -> service.withdraw(accountId, request, token));
    }

    @Test
    void withdraw_fails_when_daily_limit_exceeded() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String token = "token";

        DepositRequest request = DepositRequest.builder()
                .amount(new BigDecimal("200.00"))
                .description("Retiro cajero automático")
                .build();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setCustomerId(customerId);
        accountEntity.setBalance(new BigDecimal("2000.00"));
        accountEntity.setDailyWithdrawalLimit(new BigDecimal("1000"));

        when(repositoryAccount.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());

        LocalDate currentDateUtc = LocalDate.now(ZoneOffset.UTC);
        Instant start = currentDateUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = currentDateUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        when(repositoryTransaction.sumAmountByAccountAndTypeAndStatusBetween(
                eq(accountId),
                eq(TransactionType.WITHDRAW),
                eq(TransactionStatus.COMPLETED),
                eq(start),
                eq(end)
        )).thenReturn(new BigDecimal("900.00"));

        assertThrows(DailyWithdrawalLimitExceededException.class, () -> service.withdraw(accountId, request, token));
    }

}