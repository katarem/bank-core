package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.util.IbanUtil;
import com.bytecodes.ms_accounts.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
