package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.handler.exceptions.UserNotFoundException;
import com.bytecodes.ms_accounts.mapper.AccountMapper;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.response.AccountSummary;
import com.bytecodes.ms_accounts.response.CustomerValidationResponse;
import com.bytecodes.ms_accounts.util.IbanUtil;
import com.bytecodes.ms_accounts.util.JwtUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Integer MAX_ACCOUNT_BY_CLIENT = 3;

    private final AccountRepository repository;
    private final AccountMapper mapper = AccountMapper.INSTANCE;
    private final JwtUtil jwtUtil;
    private final IbanUtil ibanUtil;
    private final CustomerClient customerClient;

    public Account registerAccount(final Account account, final String token) {
        UUID customerId = UUID.fromString((String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID));

        CustomerValidationResponse customerValidationResponse = customerClient.validateCustomer(customerId);
        if (!customerValidationResponse.isActive()) {
            throw new CustomerIsInactiveException();
        }

        //Maximo 3 cuentas por cliente
        long count = repository.countByCustomerId(customerId);
        if (count >= MAX_ACCOUNT_BY_CLIENT) {
            throw new CreateAccountLimitExceededException();
        }

        AccountEntity entity = mapper.toEntity(account);
        entity.setCustomerId(customerId);
        entity.setAccountNumber(generateIban());
        entity.setDailyWithdrawalLimit(BigDecimal.valueOf(1000));

        AccountEntity created = repository.save(entity);

        return mapper.toModel(created);
    }

    public Account getAccount(final UUID accountId, final String token) {
        AccountEntity account = repository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        String customerId = (String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);
        if (!customerId.equals(account.getCustomerId().toString())) {
            throw new NotOwnAccountException();
        }

        return mapper.toModel(account);
    }

    public List<AccountSummary> getMyAccounts(final String token) {
        UUID customerId = UUID.fromString((String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID));
        try {
            CustomerValidationResponse customerValidationResponse = customerClient.validateCustomer(customerId);
            if (!customerValidationResponse.isExists()) {
                throw new UserNotFoundException();
            }
        } catch (FeignException.NotFound ex) {
            throw new UserNotFoundException();
        }

        List<AccountEntity> entities = repository.findAllByCustomerId(customerId);
        return entities.stream()
            .map(mapper::toSummary)
                .toList();
    }

    /**
     * Genera un IBAN y valida que no exista una cuenta con dicho IBAN
     * @return IBAN español único
     */
    private String generateIban() {
        String iban;
        do {
            iban = ibanUtil.generateSpanishIban();
        } while (repository.existsByAccountNumber(iban));

        return iban;
    }

}
