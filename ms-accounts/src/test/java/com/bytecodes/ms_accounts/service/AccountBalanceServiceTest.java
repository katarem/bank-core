package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.config.TestConfig;
import com.bytecodes.ms_accounts.dto.request.CreateTransferRequest;
import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.response.CreateTransferResponse;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.mapper.TransactionMapper;
import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.model.TransactionStatus;
import com.bytecodes.ms_accounts.model.TransactionType;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.repository.TransactionRepository;
import com.bytecodes.ms_accounts.dto.response.CustomerResponse;
import com.bytecodes.ms_accounts.dto.response.CustomerValidationResponse;
import com.bytecodes.ms_accounts.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {AccountBalanceService.class, TestConfig.class})
public class AccountBalanceServiceTest {

    @MockitoBean
    private AccountRepository repositoryAccount;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TransactionRepository repositoryTransaction;

    @MockitoBean
    private CustomerClient client;

    @Autowired
    private TransactionMapper mapper;

    @Autowired
    private AccountBalanceService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "FEE", BigDecimal.ZERO);
    }


    @Test
    void deposit_ok() {
        // given
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        AuthPrincipal authentication = auth(customerId);

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
        DepositResponse response = service.deposit(accountId, request, authentication);

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
        AuthPrincipal authentication = auth(customerId);
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
                .thenReturn(Optional.of(accountEntity))
                .thenThrow(new RuntimeException("db error"));

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
        DepositResponse response = service.deposit(accountId, request, authentication);

        // then
        assertNotNull(response);
        assertEquals(initialBalance, response.getBalanceAfter());
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(repositoryTransaction, times(2)).save(captor.capture());
        assertEquals(TransactionStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void create_transfer_ok_when_destination_belongs_to_same_customer() {
        // given
        UUID customerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), customerId, "ES1111111111111111111111", "1000.00");
        AccountEntity destination = accountEntity(UUID.randomUUID(), customerId, "ES2222222222222222222222", "150.00");
        CreateTransferRequest request = transferRequest(source.getId(), destination.getAccountNumber(), "100.00");
        AuthPrincipal authentication = auth(customerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.of(destination));
        when(client.validateCustomer(customerId)).thenReturn(activeCustomerValidation());
        when(client.getCustomer(customerId)).thenReturn(customerResponse(customerId, "Alice"));
        when(repositoryTransaction.saveAll(any())).thenAnswer(invocation -> {
            List<TransactionEntity> txs = invocation.getArgument(0);
            txs.forEach(tx -> {
                if (tx.getId() == null) {
                    tx.setId(UUID.randomUUID());
                }
            });
            return txs;
        });

        // when
        CreateTransferResponse response = service.createTransfer(request, authentication);

        // then
        assertNotNull(response);
        assertNotNull(response.getTransferId());
        assertEquals("COMPLETED", response.getStatus().name());
        assertEquals("Alice", response.getBeneficiaryName());
        assertEquals(source.getAccountNumber(), response.getSourceAccount());
        assertEquals(destination.getAccountNumber(), response.getDestinationAccount());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(new BigDecimal("100.00"), response.getTotalDebited());
        assertEquals(BigDecimal.ZERO, response.getFee());

        ArgumentCaptor<List<TransactionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repositoryTransaction, times(2)).saveAll(captor.capture());
        List<List<TransactionEntity>> persisted = captor.getAllValues();
        assertEquals(2, persisted.size());
        assertEquals(2, persisted.get(0).size());
        assertEquals(TransactionStatus.COMPLETED, persisted.get(1).get(0).getStatus());
        assertEquals(TransactionStatus.COMPLETED, persisted.get(1).get(1).getStatus());
        assertEquals(TransactionType.TRANSFER_OUT, persisted.get(0).get(0).getType());
        assertEquals(TransactionType.TRANSFER_IN, persisted.get(0).get(1).getType());

        verify(client, times(1)).validateCustomer(customerId);
        verify(client, times(1)).getCustomer(customerId);
    }

    @Test
    void create_transfer_ok_when_destination_belongs_to_other_customer() {
        // given
        UUID sourceCustomerId = UUID.randomUUID();
        UUID destinationCustomerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), sourceCustomerId, "ES1111111111111111111111", "500.00");
        AccountEntity destination = accountEntity(UUID.randomUUID(), destinationCustomerId, "ES2222222222222222222222", "50.00");
        CreateTransferRequest request = transferRequest(source.getId(), destination.getAccountNumber(), "100.00");
        AuthPrincipal authentication = auth(sourceCustomerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.of(destination));
        when(client.validateCustomer(sourceCustomerId)).thenReturn(activeCustomerValidation());
        when(client.getCustomer(sourceCustomerId)).thenReturn(customerResponse(sourceCustomerId, "Alice"));
        when(client.getCustomer(destinationCustomerId)).thenReturn(customerResponse(destinationCustomerId, "Bob"));
        when(repositoryTransaction.saveAll(any())).thenAnswer(invocation -> {
            List<TransactionEntity> txs = invocation.getArgument(0);
            txs.forEach(tx -> {
                if (tx.getId() == null) {
                    tx.setId(UUID.randomUUID());
                }
            });
            return txs;
        });

        // when
        CreateTransferResponse response = service.createTransfer(request, authentication);

        // then
        assertNotNull(response);
        assertNotNull(response.getTransferId());
        assertEquals("Bob", response.getBeneficiaryName());
        assertEquals(new BigDecimal("100.00"), response.getTotalDebited());

        ArgumentCaptor<List<TransactionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repositoryTransaction, times(2)).saveAll(captor.capture());
        List<TransactionEntity> firstSave = captor.getAllValues().get(0);
        assertEquals(new BigDecimal("-100.00"), firstSave.get(0).getAmount());
        assertEquals(new BigDecimal("100.00"), firstSave.get(1).getAmount());
        assertEquals(TransactionStatus.COMPLETED, captor.getAllValues().get(1).get(0).getStatus());
        assertEquals(TransactionStatus.COMPLETED, captor.getAllValues().get(1).get(1).getStatus());

        verify(client, times(2)).validateCustomer(sourceCustomerId);
        verify(client, times(1)).getCustomer(sourceCustomerId);
        verify(client, times(1)).getCustomer(destinationCustomerId);
    }

    @Test
    void create_transfer_marks_transactions_failed_when_balance_is_insufficient() {
        // given
        UUID customerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), customerId, "ES1111111111111111111111", "10.00");
        AccountEntity destination = accountEntity(UUID.randomUUID(), customerId, "ES2222222222222222222222", "100.00");
        CreateTransferRequest request = transferRequest(source.getId(), destination.getAccountNumber(), "50.00");
        AuthPrincipal authentication = auth(customerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.of(destination));
        when(client.validateCustomer(customerId)).thenReturn(activeCustomerValidation());
        when(client.getCustomer(customerId)).thenReturn(customerResponse(customerId, "Alice"));
        when(repositoryTransaction.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateTransferResponse response = service.createTransfer(request, authentication);

        // then
        assertNotNull(response);
        ArgumentCaptor<List<TransactionEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repositoryTransaction, times(2)).saveAll(captor.capture());
        assertEquals(TransactionStatus.FAILED, captor.getAllValues().get(1).get(0).getStatus());
        assertEquals(TransactionStatus.FAILED, captor.getAllValues().get(1).get(1).getStatus());
    }

    @Test
    void create_transfer_throws_when_source_account_does_not_exist() {
        // given
        UUID customerId = UUID.randomUUID();
        CreateTransferRequest request = transferRequest(UUID.randomUUID(), "ES2222222222222222222222", "50.00");
        AuthPrincipal authentication = auth(customerId);
        when(repositoryAccount.findById(request.getSourceAccountId())).thenReturn(Optional.empty());

        // when + then
        assertThrows(AccountNotFoundException.class, () -> service.createTransfer(request, authentication));
        verify(repositoryAccount).findById(request.getSourceAccountId());
        verify(repositoryAccount, never()).findOne(any(Example.class));
        verifyNoInteractions(repositoryTransaction, client);
    }

    @Test
    void create_transfer_throws_when_destination_account_does_not_exist() {
        // given
        UUID customerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), customerId, "ES1111111111111111111111", "200.00");
        CreateTransferRequest request = transferRequest(source.getId(), "ES9999999999999999999999", "50.00");
        AuthPrincipal authentication = auth(customerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.empty());

        // when + then
        assertThrows(AccountNotFoundException.class, () -> service.createTransfer(request, authentication));
        verify(repositoryAccount).findById(source.getId());
        verify(repositoryAccount).findOne(any(Example.class));
        verifyNoInteractions(repositoryTransaction, client);
    }

    @Test
    void create_transfer_throws_when_destination_customer_validation_fails() {
        // given
        UUID sourceCustomerId = UUID.randomUUID();
        UUID destinationCustomerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), sourceCustomerId, "ES1111111111111111111111", "500.00");
        AccountEntity destination = accountEntity(UUID.randomUUID(), destinationCustomerId, "ES2222222222222222222222", "20.00");
        CreateTransferRequest request = transferRequest(source.getId(), destination.getAccountNumber(), "10.00");
        AuthPrincipal authentication = auth(sourceCustomerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.of(destination));
        when(client.validateCustomer(sourceCustomerId)).thenReturn(
                CustomerValidationResponse.builder().exists(false).isActive(false).build());

        // when + then
        assertThrows(UsernameNotFoundException.class, () -> service.createTransfer(request, authentication));
        verify(client, times(1)).validateCustomer(sourceCustomerId);
        verifyNoInteractions(repositoryTransaction);
    }

    @Test
    void create_transfer_throws_when_source_customer_is_inactive() {
        // given
        UUID sourceCustomerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), sourceCustomerId, "ES1111111111111111111111", "500.00");
        AccountEntity destination = accountEntity(UUID.randomUUID(), sourceCustomerId, "ES2222222222222222222222", "20.00");
        CreateTransferRequest request = transferRequest(source.getId(), destination.getAccountNumber(), "10.00");
        AuthPrincipal authentication = auth(sourceCustomerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.of(destination));
        when(client.validateCustomer(sourceCustomerId)).thenReturn(
                CustomerValidationResponse.builder().exists(true).isActive(false).build());

        // when + then
        assertThrows(UsernameNotFoundException.class, () -> service.createTransfer(request, authentication));
        verify(client, times(1)).validateCustomer(sourceCustomerId);
        verifyNoInteractions(repositoryTransaction);
    }

    @Test
    void create_transfer_throws_when_source_account_is_not_owned_by_authenticated_customer() {
        // given
        UUID ownerCustomerId = UUID.randomUUID();
        UUID requestCustomerId = UUID.randomUUID();
        AccountEntity source = accountEntity(UUID.randomUUID(), ownerCustomerId, "ES1111111111111111111111", "500.00");
        AccountEntity destination = accountEntity(UUID.randomUUID(), requestCustomerId, "ES2222222222222222222222", "20.00");
        CreateTransferRequest request = transferRequest(source.getId(), destination.getAccountNumber(), "10.00");
        AuthPrincipal authentication = auth(requestCustomerId);

        when(repositoryAccount.findById(source.getId())).thenReturn(Optional.of(source));
        when(repositoryAccount.findOne(any(Example.class))).thenReturn(Optional.of(destination));
        when(client.validateCustomer(requestCustomerId)).thenReturn(activeCustomerValidation());

        // when + then
        assertThrows(NotOwnAccountException.class, () -> service.createTransfer(request, authentication));
        verify(client, times(1)).validateCustomer(requestCustomerId);
        verifyNoInteractions(repositoryTransaction);
        verify(client, never()).getCustomer(any());
    }

    private CreateTransferRequest transferRequest(UUID sourceAccountId, String destinationAccountNumber, String amount) {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceAccountId(sourceAccountId);
        request.setDestinationAccountNumber(destinationAccountNumber);
        request.setAmount(new BigDecimal(amount));
        request.setConcept("Transfer test");
        return request;
    }

    private AccountEntity accountEntity(UUID id, UUID customerId, String accountNumber, String balance) {
        AccountEntity entity = new AccountEntity();
        entity.setId(id);
        entity.setCustomerId(customerId);
        entity.setAccountNumber(accountNumber);
        entity.setBalance(new BigDecimal(balance));
        return entity;
    }

    private CustomerValidationResponse activeCustomerValidation() {
        return CustomerValidationResponse.builder()
                .exists(true)
                .isActive(true)
                .build();
    }

    private CustomerResponse customerResponse(UUID customerId, String fullName) {
        return CustomerResponse.builder()
                .id(customerId.toString())
                .fullName(fullName)
                .status("ACTIVE")
                .build();
    }


    private AuthPrincipal auth(UUID customerId) {
        AuthPrincipal authentication = new AuthPrincipal();
        authentication.setUsername("user@test.com");
        authentication.setCustomerId(customerId);
        return authentication;
    }
}
