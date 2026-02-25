package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.mapper.TransactionMapper;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.model.TransactionStatus;
import com.bytecodes.ms_accounts.model.TransactionType;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.repository.TransactionRepository;
import com.bytecodes.ms_accounts.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Se crea un nuevo service para administrar el balance de manera independiente
 * y en caso que falle algo se maneje en una nueva transacción. Con esto garantizamos
 * que la entidad TransactionEntity pueda manejar históricos con FAILED y COMPLETED.
 *
 * Esto toca en un nuevo service dado que Spring maneja @Transactional con proxies:
 *  Cuando un método del mismo bean llama a otro método del mismo bean:
 *  -NO pasa por el proxy
 *  -NO se crea nueva transacción
 *  -REQUIRES_NEW es ignorado
 *
 */
@Service
@RequiredArgsConstructor
public class AccountBalanceService {

    private final TransactionRepository repositoryTransaction;
    private final AccountRepository repositoryAccount;
    private final JwtUtil jwtUtil;
    private final TransactionMapper mapperTransaction =  TransactionMapper.INSTANCE;

    public DepositResponse deposit(final UUID accountId, final DepositRequest request, final String token) {
        AccountEntity account = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        checkOwnerAccount(token, account);

        BigDecimal amount = request.getAmount();
        BigDecimal balanceBefore = account.getBalance();

        //1 - Creamos la transacción en PENDIENTE
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .accountId(accountId)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.add(amount))
                .concept(request.getDescription())
                .status(TransactionStatus.PENDING)
                .build();

        repositoryTransaction.save(transactionEntity);

        //Con el fin de garantizar que exista un histórico el depósito se realiza en una nueva transacción
        try {
            //2 - Intenta aplicar el depósito
            AccountEntity accountUpdated = this.applyDeposit(accountId, amount);
            //3.1 - Actualizamos transacción a COMPLETE
            transactionEntity.setStatus(TransactionStatus.COMPLETED);
            //Obtenemos el saldo directamente desde la entidad
            transactionEntity.setBalanceAfter(accountUpdated.getBalance());

        } catch (Exception e) {
            //3.2 - Actualizamos transacción a FAILED
            //Dejamos el balance inicial y marcamos la transacción como fallida
            transactionEntity.setBalanceAfter(balanceBefore);
            transactionEntity.setStatus(TransactionStatus.FAILED);

        }

        repositoryTransaction.save(transactionEntity);

        return mapperTransaction.toDepositResponse(mapperTransaction.toModel(transactionEntity));

    }

    /**
     * Verifica que la cuenta proporcionada pertenezca al cliente autenticado.
     * @param token Token JWT del cual se extrae el identificador del cliente.
     * @param account Entidad de cuenta que se desea validar.
     */
    private void checkOwnerAccount(String token, AccountEntity account) {
        String customerId = (String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);
        if (!customerId.equals(account.getCustomerId().toString())) {
            throw new NotOwnAccountException();
        }
    }

    /**
     * Se encarga de realizar un deposito en un "hilo"/transacción nueva. En caso de fallo lanzará una
     * excepción.
     * @param accountId Identificador de la cuenta
     * @param amount Monto a depositar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)//Se ejecuta en una nueva transacción, en caso de que falle, solo se hará rollback de esta Tx. Con esto garantizamos que Transaction siempre quede ya sea con estatus COMPLETED o FAILED
    private AccountEntity applyDeposit(UUID accountId, BigDecimal amount) {
        AccountEntity accountEntity = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        accountEntity.setBalance(accountEntity.getBalance().add(amount));

        //Lo siguiente no es necesario dado que estamos en una transacción y está managed. Por tanto, automáticamente hibérnate se encagará de hacerlo. Esto es conocido como: "Dirty checking"
        //repositoryAccount.save(accountEntity);

        return accountEntity;
    }

}
