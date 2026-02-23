package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.handler.AccountExceptionHandler;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.handler.exceptions.UserNotFoundException;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
import com.bytecodes.ms_accounts.response.AccountSummary;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.List;
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

    private String userToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        userToken = TokenUtil.generateToken("super-mega-complex-secret-very-complex", "user", UUID.randomUUID());
    }

    @Test
    void register_account_returns_created() throws Exception {
        //given
        Account account = Account.builder()
                .accountType(AccountType.CHECKING)
                .currency("EUR")
                .alias("Mi cuenta corriente")
                .build();

        //when
        Mockito.when(service.registerAccount(Mockito.any(), Mockito.isNotNull()))
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
        Account account = Account.builder()
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
    void get_account_ok() throws Exception {
        // given
        Account acc = new Account();
        acc.setId(UUID.randomUUID());
        // when
        Mockito.when(service.getAccount(acc.getId(), "Bearer " + userToken))
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
        Mockito.when(service.getAccount(accountId, userToken))//En el llamado a la capa service se debe realizar sin el "Bearer "
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
        Mockito.when(service.getAccount(accountId, userToken))//El llamado a la capa service lo realiza sin el "Bearer ", dado que el controller lo quita
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

        Mockito.when(service.getMyAccounts(userToken))
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
        Mockito.when(service.getMyAccounts(userToken))
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
        Mockito.when(service.getMyAccounts(userToken))
                .thenThrow(new UserNotFoundException());

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/accounts")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No existe el usuario"));
    }

    @Test
    void get_my_accounts_without_token_returns_bad_request() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/accounts")
                )
                .andExpect(status().isBadRequest());
    }


}
