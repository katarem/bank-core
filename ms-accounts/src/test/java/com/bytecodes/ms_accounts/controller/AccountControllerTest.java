package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.handler.CustomerExceptionHandler;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.service.AccountService;
import com.bytecodes.ms_accounts.util.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(CustomerExceptionHandler.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService service;

    private String userToken;

    @BeforeEach
    void setup() {
        userToken = TokenUtil.generateToken("super-mega-complex-secret-very-complex", "user", UUID.randomUUID());
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
        Mockito.when(service.getAccount(accountId, "Bearer " + userToken))
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
        Mockito.when(service.getAccount(accountId, "Bearer " + userToken))
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


}
