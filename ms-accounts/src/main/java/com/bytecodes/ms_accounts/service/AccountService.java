package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.dto.request.RegisterAccountRequest;
import com.bytecodes.ms_accounts.dto.response.GetAccountResponse;
import com.bytecodes.ms_accounts.dto.response.RegisterAccountResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.mapper.AccountMapper;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.dto.response.CustomerValidationResponse;
import com.bytecodes.ms_accounts.util.IbanUtil;
import com.bytecodes.ms_accounts.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Integer MAX_ACCOUNT_BY_CLIENT = 3;

    private final AccountRepository repositoryAccount;
    private final AccountMapper mapper;
    private final IbanUtil ibanUtil;
    private final CustomerClient customerClient;

    public RegisterAccountResponse registerAccount(final RegisterAccountRequest request, final AuthPrincipal authentication) {
        CustomerValidationResponse customerValidationResponse = customerClient.validateCustomer(authentication.getCustomerId());
        if (!customerValidationResponse.isActive()) {
            throw new CustomerIsInactiveException();
        }

        //Maximo 3 cuentas por cliente
        long count = repositoryAccount.countByCustomerId(authentication.getCustomerId());
        if (count >= MAX_ACCOUNT_BY_CLIENT) {
            throw new CreateAccountLimitExceededException();
        }

        Account account = mapper.toModel(request);
        AccountEntity entity = mapper.toEntity(account);

        entity.setCustomerId(authentication.getCustomerId());
        entity.setAccountNumber(generateIban());
        entity.setDailyWithdrawalLimit(BigDecimal.valueOf(1000));

        AccountEntity created = repositoryAccount.save(entity);

        Account model = mapper.toModel(created);

        return mapper.toRegisterResponse(model);
    }

    public GetAccountResponse getAccount(final UUID accountId, final AuthPrincipal authentication) {
        AccountEntity entity = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        if (!authentication.getCustomerId().equals(entity.getCustomerId())) {
            throw new NotOwnAccountException();
        }

        Account account = mapper.toModel(entity);

        return mapper.toGetAccountResponse(account);
    }

    /**
     * Genera un IBAN y valida que no exista una cuenta con dicho IBAN
     * @return IBAN español único
     */
    private String generateIban() {
        String iban;
        do {
            iban = ibanUtil.generateSpanishIban();
        } while (repositoryAccount.existsByAccountNumber(iban));

        return iban;
    }

}
