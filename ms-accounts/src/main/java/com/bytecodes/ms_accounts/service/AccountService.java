package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CreateAccountLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.mapper.AccountMapper;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.model.TransactionStatus;
import com.bytecodes.ms_accounts.model.TransactionType;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.repository.TransactionRepository;
import com.bytecodes.ms_accounts.response.CustomerValidationResponse;
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
    private final TransactionRepository repositoryTransaction;
    private final AccountMapper mapper = AccountMapper.INSTANCE;
    private final JwtUtil jwtUtil;
    private final IbanUtil ibanUtil;
    private final CustomerClient customerClient;
    private final AccountBalanceService accountBalanceService;

    public DepositResponse deposit(final UUID accountId, final DepositRequest request, final String token) {
        Account account = this.getAccount(accountId, token);//Se garantiza que la cuenta exista y sea del cliente

        BigDecimal amount = request.getAmount();
        BigDecimal balanceBefore = account.getBalance();

        //1 - Creamos la transacción en PENDIENTE
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .accountId(accountId)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(balanceBefore.add(amount))
                .concept(request.getDescription())
                .status(TransactionStatus.PENDING)
                .build();

        repositoryTransaction.save(transactionEntity);

        //Con el fin de garantizar que exista un historico el desposito se realiza en una nueva transaccion
        try {
            //2 - Intenta aplicar el depósito
            accountBalanceService.applyDeposit(accountId, amount);
            //3.1 - Actualizamos transacciÓn a COMPLETE
            transactionEntity.setStatus(TransactionStatus.COMPLETED);

            //Recalculamos saldo real desde DB
            AccountEntity accountUpdated = repositoryAccount.findById(accountId).get();
            transactionEntity.setBalanceAfter(accountUpdated.getBalance());

        } catch (Exception e) {
            //3.2 - Actualizamos transacción a FAILED
            //Dejamos el balance inicial y marcamos la transacción como fallida
            transactionEntity.setBalanceAfter(balanceBefore);
            transactionEntity.setStatus(TransactionStatus.FAILED);

        }

        repositoryTransaction.save(transactionEntity);

        return DepositResponse.builder()
                .transactionId(transactionEntity.getId())
                .type(transactionEntity.getType())
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(transactionEntity.getBalanceAfter())
                .description(transactionEntity.getConcept())
                .timestamp(transactionEntity.getCreatedAt())
                .build();

    }

    public Account registerAccount(final Account account, final String token) {
        UUID customerId = UUID.fromString((String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID));

        CustomerValidationResponse customerValidationResponse = customerClient.validateCustomer(customerId);
        if (!customerValidationResponse.isActive()) {
            throw new CustomerIsInactiveException();
        }

        //Maximo 3 cuentas por cliente
        long count = repositoryAccount.countByCustomerId(customerId);
        if (count >= MAX_ACCOUNT_BY_CLIENT) {
            throw new CreateAccountLimitExceededException();
        }

        AccountEntity entity = mapper.toEntity(account);
        entity.setCustomerId(customerId);
        entity.setAccountNumber(generateIban());
        entity.setDailyWithdrawalLimit(BigDecimal.valueOf(1000));

        AccountEntity created = repositoryAccount.save(entity);

        return mapper.toModel(created);
    }

    public Account getAccount(final UUID accountId, final String token) {
        AccountEntity account = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        String customerId = (String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);
        if (!customerId.equals(account.getCustomerId().toString())) {
            throw new NotOwnAccountException();
        }

        return mapper.toModel(account);
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
