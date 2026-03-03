package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.dto.request.RegisterAccountRequest;
import com.bytecodes.ms_accounts.dto.response.CustomerValidationResponse;
import com.bytecodes.ms_accounts.dto.response.GetAccountResponse;
import com.bytecodes.ms_accounts.dto.response.RegisterAccountResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.mapper.AccountMapper;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.util.IbanUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private IbanUtil ibanUtil;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private AccountMapper mapper;

    @InjectMocks
    private AccountService service;

    @Test
    void create_account_ok() {
        UUID customerId = UUID.randomUUID();
        RegisterAccountRequest request = registerRequest(AccountType.SAVINGS, "EUR", "Mi cuenta de ahorros");
        Account mappedRequest = Account.builder()
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .alias(request.getAlias())
                .build();

        AccountEntity databaseAccount = AccountEntity.builder()
                .id(UUID.randomUUID())
                .accountNumber("ES5345568154925150625381")
                .customerId(customerId)
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .alias(request.getAlias())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("1000"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Account savedModel = Account.builder()
                .id(databaseAccount.getId())
                .accountNumber(databaseAccount.getAccountNumber())
                .customerId(databaseAccount.getCustomerId())
                .accountType(databaseAccount.getAccountType())
                .currency(databaseAccount.getCurrency())
                .alias(databaseAccount.getAlias())
                .balance(databaseAccount.getBalance())
                .status(databaseAccount.getStatus())
                .dailyWithdrawalLimit(databaseAccount.getDailyWithdrawalLimit())
                .createdAt(databaseAccount.getCreatedAt())
                .updatedAt(databaseAccount.getUpdatedAt())
                .build();
        RegisterAccountResponse mappedResponse = RegisterAccountResponse.builder()
                .id(savedModel.getId())
                .accountNumber(savedModel.getAccountNumber())
                .customerId(savedModel.getCustomerId())
                .accountType(savedModel.getAccountType())
                .currency(savedModel.getCurrency())
                .alias(savedModel.getAlias())
                .balance(savedModel.getBalance())
                .status(savedModel.getStatus())
                .dailyWithdrawalLimit(savedModel.getDailyWithdrawalLimit())
                .createdAt(savedModel.getCreatedAt())
                .updatedAt(savedModel.getUpdatedAt())
                .build();

        when(customerClient.validateCustomer(customerId)).thenReturn(activeCustomerValidation());
        when(repository.countByCustomerId(customerId)).thenReturn(0L);
        when(ibanUtil.generateSpanishIban()).thenReturn(databaseAccount.getAccountNumber());
        when(repository.existsByAccountNumber(databaseAccount.getAccountNumber())).thenReturn(false);
        when(mapper.toModel(request)).thenReturn(mappedRequest);
        when(mapper.toEntity(mappedRequest)).thenReturn(new AccountEntity());
        when(repository.save(any(AccountEntity.class))).thenReturn(databaseAccount);
        when(mapper.toModel(databaseAccount)).thenReturn(savedModel);
        when(mapper.toRegisterResponse(savedModel)).thenReturn(mappedResponse);

        RegisterAccountResponse accountRegistered = service.registerAccount(request, auth(customerId));

        assertNotNull(accountRegistered);
        assertEquals(databaseAccount.getId(), accountRegistered.getId());
        assertEquals(databaseAccount.getAccountNumber(), accountRegistered.getAccountNumber());
        assertEquals(request.getAccountType(), accountRegistered.getAccountType());
        assertEquals(request.getCurrency(), accountRegistered.getCurrency());
        assertEquals(request.getAlias(), accountRegistered.getAlias());
        assertEquals(BigDecimal.ZERO, accountRegistered.getBalance());
        assertEquals(AccountStatus.ACTIVE, accountRegistered.getStatus());
        assertNotNull(accountRegistered.getCreatedAt());
        assertNotNull(accountRegistered.getUpdatedAt());
    }

    @Test
    void create_account_customer_not_active() {
        UUID customerId = UUID.randomUUID();
        RegisterAccountRequest request = registerRequest(AccountType.SAVINGS, "EUR", "Mi cuenta de ahorros");

        when(customerClient.validateCustomer(customerId)).thenReturn(
                CustomerValidationResponse.builder().exists(true).isActive(false).build());

        assertThrows(CustomerIsInactiveException.class, () -> service.registerAccount(request, auth(customerId)));
    }

    @Test
    void create_account_customer_have_three_accounts() {
        UUID customerId = UUID.randomUUID();

        when(customerClient.validateCustomer(customerId)).thenReturn(activeCustomerValidation());
        when(repository.countByCustomerId(customerId)).thenReturn(3L);

        assertThrows(CreateAccountLimitExceededException.class,
                () -> service.registerAccount(registerRequest(AccountType.CHECKING, "EUR", "Cuenta"), auth(customerId)));
    }

    @Test
    void create_account_with_two_accounts_allowed() {
        UUID customerId = UUID.randomUUID();
        RegisterAccountRequest request = registerRequest(AccountType.CHECKING, "EUR", "Cuenta");
        Account mappedRequest = Account.builder()
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .alias(request.getAlias())
                .build();
        AccountEntity createdEntity = AccountEntity.builder()
                .id(UUID.randomUUID())
                .accountNumber("ES5345568154925150625381")
                .customerId(customerId)
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .alias(request.getAlias())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("1000"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Account createdModel = Account.builder()
                .id(createdEntity.getId())
                .accountNumber(createdEntity.getAccountNumber())
                .customerId(createdEntity.getCustomerId())
                .accountType(createdEntity.getAccountType())
                .currency(createdEntity.getCurrency())
                .alias(createdEntity.getAlias())
                .balance(createdEntity.getBalance())
                .status(createdEntity.getStatus())
                .dailyWithdrawalLimit(createdEntity.getDailyWithdrawalLimit())
                .createdAt(createdEntity.getCreatedAt())
                .updatedAt(createdEntity.getUpdatedAt())
                .build();
        RegisterAccountResponse response = RegisterAccountResponse.builder()
                .id(createdModel.getId())
                .accountNumber(createdModel.getAccountNumber())
                .customerId(createdModel.getCustomerId())
                .accountType(createdModel.getAccountType())
                .currency(createdModel.getCurrency())
                .alias(createdModel.getAlias())
                .balance(createdModel.getBalance())
                .status(createdModel.getStatus())
                .dailyWithdrawalLimit(createdModel.getDailyWithdrawalLimit())
                .createdAt(createdModel.getCreatedAt())
                .updatedAt(createdModel.getUpdatedAt())
                .build();

        when(customerClient.validateCustomer(customerId)).thenReturn(activeCustomerValidation());
        when(repository.countByCustomerId(customerId)).thenReturn(2L);
        when(ibanUtil.generateSpanishIban()).thenReturn("ES5345568154925150625381");
        when(repository.existsByAccountNumber("ES5345568154925150625381")).thenReturn(false);
        when(mapper.toModel(request)).thenReturn(mappedRequest);
        when(mapper.toEntity(mappedRequest)).thenReturn(new AccountEntity());
        when(repository.save(any(AccountEntity.class))).thenReturn(createdEntity);
        when(mapper.toModel(createdEntity)).thenReturn(createdModel);
        when(mapper.toRegisterResponse(createdModel)).thenReturn(response);

        RegisterAccountResponse accountRegistered =
                service.registerAccount(request, auth(customerId));

        assertNotNull(accountRegistered);
        verify(repository).save(any(AccountEntity.class));
    }

    @Test
    void create_account_iban_collision_retries_until_unique() {
        UUID customerId = UUID.randomUUID();
        RegisterAccountRequest request = registerRequest(AccountType.CHECKING, "EUR", "Cuenta");
        Account mappedRequest = Account.builder()
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .alias(request.getAlias())
                .build();
        AccountEntity createdEntity = AccountEntity.builder()
                .id(UUID.randomUUID())
                .accountNumber("ES1111111111111111111111")
                .customerId(customerId)
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .alias(request.getAlias())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("1000"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Account createdModel = Account.builder()
                .id(createdEntity.getId())
                .accountNumber(createdEntity.getAccountNumber())
                .customerId(createdEntity.getCustomerId())
                .accountType(createdEntity.getAccountType())
                .currency(createdEntity.getCurrency())
                .alias(createdEntity.getAlias())
                .balance(createdEntity.getBalance())
                .status(createdEntity.getStatus())
                .dailyWithdrawalLimit(createdEntity.getDailyWithdrawalLimit())
                .createdAt(createdEntity.getCreatedAt())
                .updatedAt(createdEntity.getUpdatedAt())
                .build();
        RegisterAccountResponse response = RegisterAccountResponse.builder()
                .id(createdModel.getId())
                .accountNumber(createdModel.getAccountNumber())
                .customerId(createdModel.getCustomerId())
                .accountType(createdModel.getAccountType())
                .currency(createdModel.getCurrency())
                .alias(createdModel.getAlias())
                .balance(createdModel.getBalance())
                .status(createdModel.getStatus())
                .dailyWithdrawalLimit(createdModel.getDailyWithdrawalLimit())
                .createdAt(createdModel.getCreatedAt())
                .updatedAt(createdModel.getUpdatedAt())
                .build();

        when(customerClient.validateCustomer(customerId)).thenReturn(activeCustomerValidation());
        when(repository.countByCustomerId(customerId)).thenReturn(0L);
        when(ibanUtil.generateSpanishIban())
                .thenReturn("ES0000000000000000000000", "ES1111111111111111111111");
        when(repository.existsByAccountNumber("ES0000000000000000000000")).thenReturn(Boolean.TRUE);
        when(repository.existsByAccountNumber("ES1111111111111111111111")).thenReturn(Boolean.FALSE);
        when(mapper.toModel(request)).thenReturn(mappedRequest);
        when(mapper.toEntity(mappedRequest)).thenReturn(new AccountEntity());
        when(repository.save(any(AccountEntity.class))).thenReturn(createdEntity);
        when(mapper.toModel(createdEntity)).thenReturn(createdModel);
        when(mapper.toRegisterResponse(createdModel)).thenReturn(response);

        RegisterAccountResponse accountRegistered =
                service.registerAccount(request, auth(customerId));

        assertNotNull(accountRegistered);
        verify(ibanUtil, times(2)).generateSpanishIban();
        verify(repository).existsByAccountNumber("ES0000000000000000000000");
        verify(repository).existsByAccountNumber("ES1111111111111111111111");
    }

    @Test
    void get_account_by_id_ok() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(customerId);
        Account model = Account.builder().id(accountId).customerId(customerId).build();
        GetAccountResponse mappedResponse = GetAccountResponse.builder()
                .id(accountId)
                .customerId(customerId)
                .build();

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));
        when(mapper.toModel(entity)).thenReturn(model);
        when(mapper.toGetAccountResponse(model)).thenReturn(mappedResponse);

        GetAccountResponse result = service.getAccount(accountId, auth(customerId));

        assertNotNull(result);
        assertEquals(accountId, result.getId());
        assertEquals(customerId, result.getCustomerId());
        verify(repository).findById(accountId);
    }

    @Test
    void get_account_by_id_not_found() {
        UUID accountId = UUID.randomUUID();
        when(repository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getAccount(accountId, auth(UUID.randomUUID())));

        verify(repository).findById(accountId);
    }

    @Test
    void get_account_by_id_invalid_access() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        AccountEntity entity = new AccountEntity();
        entity.setId(accountId);
        entity.setCustomerId(ownerId);

        when(repository.findById(accountId)).thenReturn(Optional.of(entity));

        assertThrows(NotOwnAccountException.class, () -> service.getAccount(accountId, auth(requesterId)));

        verify(repository).findById(accountId);
    }

    private RegisterAccountRequest registerRequest(AccountType type, String currency, String alias) {
        return RegisterAccountRequest.builder()
                .accountType(type)
                .currency(currency)
                .alias(alias)
                .build();
    }

    private CustomerValidationResponse activeCustomerValidation() {
        return CustomerValidationResponse.builder()
                .exists(true)
                .isActive(true)
                .build();
    }

    private AuthPrincipal auth(UUID customerId) {
        AuthPrincipal auth = new AuthPrincipal();
        auth.setUsername("username");
        auth.setCustomerId(customerId);
        return auth;
    }
}
