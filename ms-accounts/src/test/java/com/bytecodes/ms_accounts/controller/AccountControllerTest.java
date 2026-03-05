package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.request.RegisterAccountRequest;
import com.bytecodes.ms_accounts.dto.response.AccountSummary;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.dto.response.GetAccountResponse;
import com.bytecodes.ms_accounts.dto.response.RegisterAccountResponse;
import com.bytecodes.ms_accounts.handler.AccountExceptionHandler;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.model.TransactionType;
import com.bytecodes.ms_accounts.service.AccountBalanceService;
import com.bytecodes.ms_accounts.service.AccountService;
import com.bytecodes.ms_accounts.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(AccountExceptionHandler.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService service;

    @MockitoBean
    private AccountBalanceService serviceAccountBalance;

    private String userToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        userToken = TokenUtil.generateToken("super-mega-complex-secret-very-complex", "user", UUID.randomUUID());
    }

    @Test
    void register_account_returns_created() throws Exception {
        //given
        RegisterAccountResponse account = RegisterAccountResponse.builder()
                .accountType(AccountType.CHECKING)
                .currency("EUR")
                .alias("Mi cuenta corriente")
                .build();

        //when
        Mockito.when(service.registerAccount(Mockito.any(), Mockito.any()))
                .thenReturn(account);

        //then
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(account))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType").value(account.getAccountType().toString()))
                .andExpect(jsonPath("$.currency").value(account.getCurrency()))
                .andExpect(jsonPath("$.alias").value(account.getAlias()));
    }

    @Test
    void register_account_invalid_enum_returns_bad_request() throws Exception {
        //given
        String invalidJson = """
        {
            "accountType": "INVALID_TYPE",
            "currency": "EUR",
            "alias": "Mi cuenta"
        }
        """;

        //when (En este caso no debemos configurar comportamiento, ya que no llegará a el service)

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/accounts")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @ParameterizedTest
    @MethodSource("businessRulesConflictProvider")
    void register_account_violated_business_rules_returns_conflict(RuntimeException exception) throws Exception {
        //given
        RegisterAccountResponse account = RegisterAccountResponse.builder()
                .accountType(AccountType.SAVINGS)
                .currency("USD")
                .alias("Mi cuenta")
                .build();

        //when
        Mockito.when(service.registerAccount(Mockito.any(), Mockito.any()))
                .thenThrow(exception);

        //then
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/accounts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(account))
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private static Stream<Arguments> businessRulesConflictProvider() {
        return Stream.of(
                Arguments.of(new CreateAccountLimitExceededException()),
                Arguments.of(new CustomerIsInactiveException())
        );
    }

    @Test
    void make_deposit_ok() throws Exception {
        //given
        DepositResponse response = DepositResponse.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100"))
                .balanceBefore(new BigDecimal("0"))
                .balanceAfter(new BigDecimal("200"))
                .description("Deposito en efectivo")
                .timestamp(Instant.now())
                .build();

        //when
        Mockito.when(serviceAccountBalance.deposit(Mockito.any(UUID.class), Mockito.any(DepositRequest.class), Mockito.nullable(AuthPrincipal.class))).thenReturn(response);

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/accounts/{accountId}/deposit", UUID.randomUUID())
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new DepositRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.description").value(response.getDescription()))
                .andExpect(jsonPath("$.amount").exists())
                .andExpect(jsonPath("$.balanceBefore").exists())
                .andExpect(jsonPath("$.balanceAfter").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deposit_amount_no_valid() throws Exception {
        // given
        DepositRequest request = DepositRequest.builder()
                .amount(new BigDecimal("0"))
                .build();

        // when (No entraria aqui dado que se validará antes)

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/accounts/{accountId}/deposit", UUID.randomUUID())
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FIELDS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void get_account_ok() throws Exception {
        // given
        GetAccountResponse acc = new GetAccountResponse();
        acc.setId(UUID.randomUUID());
        acc.setCustomerId(UUID.randomUUID());

        // when
        Mockito.when(service.getAccount(Mockito.eq(acc.getId()), Mockito.any()))
                .thenReturn(acc);
        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/accounts/{accountId}", acc.getId())
                        .header("Authorization", "Bearer " + userToken)
        )
                .andExpect(status().isOk());
    }

    @Test
    void get_account_not_found() throws Exception {
        // given
        UUID accountId = UUID.randomUUID();

        // when
        Mockito.when(service.getAccount(Mockito.eq(accountId), Mockito.any()))
                .thenThrow(new AccountNotFoundException(accountId.toString()));

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/accounts/{accountId}", accountId)
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void get_account_invalid_access() throws Exception {
        // given
        UUID accountId = UUID.randomUUID();

        // when
        Mockito.when(service.getAccount(Mockito.eq(accountId), Mockito.any()))
                .thenThrow(new NotOwnAccountException());

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/accounts/{accountId}", accountId)
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ACCESS_NOT_GRANTED"));
    }

    @Test
    void get_account_invalid_id() throws Exception {
        // when && then
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/accounts/{accountId}", "invalid-id")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isBadRequest());

    }

    @Test
    void get_my_accounts_ok() throws Exception {
        AccountSummary acc1 = AccountSummary.builder()
                .id(UUID.randomUUID())
                .accountNumber("ES7620770024003102575766")
                .accountType(AccountType.SAVINGS)
                .currency("EUR")
                .balance(new BigDecimal("1500.50"))
                .alias("Mi cuenta de ahorros")
                .status(AccountStatus.ACTIVE)
                .build();

        AccountSummary acc2 = AccountSummary.builder()
                .id(UUID.randomUUID())
                .accountNumber("ES9121000418450200051332")
                .accountType(AccountType.CHECKING)
                .currency("EUR")
                .balance(new BigDecimal("3200.00"))
                .alias("Cuenta nómina")
                .status(AccountStatus.ACTIVE)
                .build();

        Mockito.when(service.getMyAccounts(Mockito.nullable(AuthPrincipal.class)))
                .thenReturn(List.of(acc1, acc2));

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/accounts")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(acc1.getId().toString()))
                .andExpect(jsonPath("$[0].accountNumber").value(acc1.getAccountNumber()))
                .andExpect(jsonPath("$[0].accountType").value(acc1.getAccountType().toString()))
                .andExpect(jsonPath("$[0].currency").value(acc1.getCurrency()))
                .andExpect(jsonPath("$[0].balance").value(1500.50))
                .andExpect(jsonPath("$[0].alias").value(acc1.getAlias()))
                .andExpect(jsonPath("$[0].status").value(acc1.getStatus().toString()))
                .andExpect(jsonPath("$[0].dailyWithdrawalLimit").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$[0].customerId").doesNotExist());
    }

    @Test
    void get_my_accounts_empty_list() throws Exception {
        Mockito.when(service.getMyAccounts(Mockito.nullable(AuthPrincipal.class)))
                .thenReturn(List.of());

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/accounts")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void get_my_accounts_user_not_found() throws Exception {
        Mockito.when(service.getMyAccounts(Mockito.nullable(AuthPrincipal.class)))
                .thenThrow(new UsernameNotFoundException("El usuario no existe"));

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/accounts")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("El usuario no existe"));
    }

    @Test
        void get_my_accounts_without_token_returns_ok_with_filters_disabled() throws Exception {
                Mockito.when(service.getMyAccounts(Mockito.nullable(AuthPrincipal.class)))
                                .thenReturn(List.of());

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/accounts")
                )
                                .andExpect(status().isOk());
    }


}
