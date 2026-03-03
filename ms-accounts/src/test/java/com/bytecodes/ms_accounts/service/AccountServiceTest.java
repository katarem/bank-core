package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.dto.response.CustomerValidationResponse;
import com.bytecodes.ms_accounts.util.IbanUtil;
import com.bytecodes.ms_accounts.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
public class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private IbanUtil ibanUtil;

    @Mock
    private CustomerClient customerClient;

    @InjectMocks
    private AccountService service;

    @Test
    void create_account_ok() {
        //given
        String token = "unTokenValido";
        UUID customerId = UUID.randomUUID();

        Account account = Account.builder()
                .accountType(AccountType.SAVINGS)
                .currency("EUR")
                .alias("Mi cuenta de ahorros")
                .build();

        AccountEntity databaseAccout = AccountEntity.builder()
                .id(UUID.randomUUID())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .alias(account.getAlias())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        //when
        when(repository.save(any(AccountEntity.class))).thenReturn(databaseAccout);
        when(repository.countByCustomerId(customerId)).thenReturn(0L);
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());
        when(ibanUtil.generateSpanishIban()).thenReturn("ES5345568154925150625381");
        when(customerClient.validateCustomer(customerId)).thenReturn(
                CustomerValidationResponse.builder()
                        .exists(true)
                        .isActive(true)
                        .build());

        //then
        databaseAccout.setAccountNumber(ibanUtil.generateSpanishIban());//Le establecemos el IBAN
        Account accountRegistered = service.registerAccount(account, token);

        assertNotNull(accountRegistered);
        assertNotNull(accountRegistered.getId());
        assertNotNull(accountRegistered.getAccountNumber());
        assertEquals(account.getAccountType(), accountRegistered.getAccountType());
        assertEquals(account.getCurrency(), accountRegistered.getCurrency());
        assertEquals(account.getAlias(), accountRegistered.getAlias());
        assertEquals(BigDecimal.ZERO, accountRegistered.getBalance());
        assertEquals(AccountStatus.ACTIVE, accountRegistered.getStatus());
        assertNotNull(accountRegistered.getCreatedAt());
        assertNotNull(accountRegistered.getUpdatedAt());

    }

    @Test
    void create_account_customer_not_active() {
        //given
        String token = "unTokenValido";
        UUID customerId = UUID.randomUUID();

        Account account = Account.builder()
                .accountType(AccountType.SAVINGS)
                .currency("EUR")
                .alias("Mi cuenta de ahorros")
                .build();

        CustomerValidationResponse customerNoActive = CustomerValidationResponse.builder()
                .exists(true)
                .isActive(false)
                .build();

        //when
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());
        when(customerClient.validateCustomer(customerId)).thenReturn(customerNoActive);

        //then
        assertThrows(CustomerIsInactiveException.class, () -> service.registerAccount(account, token));

    }

    @Test
    void create_account_customer_have_three_accounts() {
        //given
        String token = "unTokenValido";
        UUID customerId = UUID.randomUUID();

        //when
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());
        when(customerClient.validateCustomer(customerId)).thenReturn(
                CustomerValidationResponse.builder()
                        .exists(true)
                        .isActive(true)
                        .build());
        when(repository.countByCustomerId(customerId)).thenReturn(3L);

        //then
        assertThrows(CreateAccountLimitExceededException.class, () -> service.registerAccount(new Account(), token));

    }

    @Test
    void create_account_with_two_accounts_allowed() {
        //given
        String token = "unTokenValido";
        UUID customerId = UUID.randomUUID();

        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());
        when(customerClient.validateCustomer(customerId)).thenReturn(
                CustomerValidationResponse.builder()
                        .exists(true)
                        .isActive(true)
                        .build());
        when(repository.countByCustomerId(customerId)).thenReturn(2L);
        when(ibanUtil.generateSpanishIban()).thenReturn("ES5345568154925150625381");
        when(repository.save(any(AccountEntity.class))).thenReturn(AccountEntity.builder().build());

        //when
        Account accountRegistered = service.registerAccount(new Account(), token);

        //then
        assertNotNull(accountRegistered);
        verify(repository).save(any(AccountEntity.class));
    }

    @Test
    void create_account_iban_collision_retries_until_unique() {
        //given
        String token = "unTokenValido";
        UUID customerId = UUID.randomUUID();

        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());
        when(customerClient.validateCustomer(customerId)).thenReturn(
                CustomerValidationResponse.builder()
                        .exists(true)
                        .isActive(true)
                        .build());
        when(repository.countByCustomerId(customerId)).thenReturn(0L);
        when(ibanUtil.generateSpanishIban())
                .thenReturn("ES0000000000000000000000", "ES1111111111111111111111");
        when(repository.existsByAccountNumber("ES0000000000000000000000")).thenReturn(Boolean.TRUE);
        when(repository.existsByAccountNumber("ES1111111111111111111111")).thenReturn(Boolean.FALSE);
        when(repository.save(any(AccountEntity.class))).thenReturn(AccountEntity.builder().build());

        //when
        Account accountRegistered = service.registerAccount(new Account(), token);

        //then
        assertNotNull(accountRegistered);
        verify(ibanUtil, Mockito.times(2)).generateSpanishIban();
        verify(repository).existsByAccountNumber("ES0000000000000000000000");
        verify(repository).existsByAccountNumber("ES1111111111111111111111");
    }

    @Test
    void create_account_invalid_token_claim_throws() {
        //given
        String token = "tokenInvalido";
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        //when && then
        assertThrows(IllegalArgumentException.class, () -> service.registerAccount(new Account(), token));
        verify(jwtUtil).extractClaim(token, JwtClaim.CUSTOMER_ID);
        verify(repository, never()).save(any(AccountEntity.class));
    }

    @Test
    void get_account_by_id_ok() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String token = "token";

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(customerId);

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(customerId.toString());

        // when
        Account result = service.getAccount(accountId, token);

        // then
        assertNotNull(result);
        assertEquals(accountId, result.getId());
        assertEquals(customerId, result.getCustomerId());
        verify(repository).findById(accountId);
        verify(jwtUtil).extractClaim(token, JwtClaim.CUSTOMER_ID);
    }

    @Test
    void get_account_by_id_not_found() {
        // given
        UUID accountId = UUID.randomUUID();
        String token = "token";
        when(repository.findById(accountId)).thenReturn(Optional.empty());

        // when
        assertThrows(AccountNotFoundException.class, () -> service.getAccount(accountId, token));

        // then
        verify(repository).findById(accountId);
        verify(jwtUtil, never()).extractClaim(any(), any());
    }

    @Test
    void get_account_by_id_invalid_access() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        String token = "token";

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(ownerId);

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(requesterId.toString());

        // when
        assertThrows(NotOwnAccountException.class, () -> service.getAccount(accountId, token));

        // then
        verify(repository).findById(accountId);
        verify(jwtUtil).extractClaim(token, JwtClaim.CUSTOMER_ID);
    }

    @Test
    void get_account_by_id_invalid_token_claim() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String token = "token";

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(ownerId);

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // when && then
        assertThrows(IllegalArgumentException.class, () -> service.getAccount(accountId, token));
        verify(repository).findById(accountId);
        verify(jwtUtil).extractClaim(token, JwtClaim.CUSTOMER_ID);
    }

    @Test
    void get_account_by_id_null_customer_claim() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String token = "token";

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(ownerId);

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(null);

        // when && then
        assertThrows(NullPointerException.class, () -> service.getAccount(accountId, token));
        verify(repository).findById(accountId);
        verify(jwtUtil).extractClaim(token, JwtClaim.CUSTOMER_ID);
    }

    @Test
    void get_account_by_id_account_with_null_owner() {
        // given
        UUID accountId = UUID.randomUUID();
        String token = "token";

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(null);

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));
        when(jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID)).thenReturn(UUID.randomUUID().toString());

        // when && then
        assertThrows(NullPointerException.class, () -> service.getAccount(accountId, token));
        verify(repository).findById(accountId);
        verify(jwtUtil).extractClaim(eq(token), eq(JwtClaim.CUSTOMER_ID));
    }

}
