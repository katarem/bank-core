package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.mapper.AccountMapper;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.util.IbanUtil;
import com.bytecodes.ms_accounts.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final AccountMapper mapper = AccountMapper.INSTANCE;
    private final JwtUtil jwtUtil;
    private final IbanUtil ibanUtil;

    public Account registerAccount(final Account account, final String token) {
        UUID customerId = UUID.fromString((String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID));

        //TODO: Validar cliente activo (usar Feign)

        //Maximo 3 cuentas
        long count = repository.countByCustomerId(customerId);
        if (count >= 3) {
            throw new RuntimeException("Máximo 3 cuentas permitidas");
        }

        AccountEntity entity = mapper.toEntity(account);
        entity.setCustomerId(customerId);
        entity.setAccountNumber(generateIban());
        entity.setDailyWithdrawalLimit(BigDecimal.valueOf(1000));

        //TODO: Validar exceptions eje: 2026-02-20T01:09:30.830+01:00  WARN 39092 --- [ms-accounts] [nio-8082-exec-5] .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.http.converter.HttpMessageNotReadableException: JSON parse error: Cannot deserialize value of type `com.bytecodes.ms_accounts.model.AccountType` from String "SAVINGSPABLO": not one of the values accepted for Enum class: [SAVINGS, CHECKING]]
        AccountEntity created = repository.save(entity);

        return mapper.toModel(created);
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
