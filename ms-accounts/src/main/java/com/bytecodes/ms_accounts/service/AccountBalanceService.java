package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Se crea un nuevo service para administrar el balance de manera independiente
 * y en caso que falle algo se maneje en una nueva transaccion. Con esto garantizamos
 * que la entidad TransactionEntity pueda manejar historicos con FAILED y COMPLETED.
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

    private final AccountRepository repositoryAccount;

    /**
     * Se encarga de realizar un deposito en un "hilo"/transacción nueva. En caso de fallo lanzará una
     * excepción.
     * @param accountId Identificador de la cuenta
     * @param amount Monto a depositar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)//Se ejecuta en una nueva transacción, en caso que falle, solo se hará rollback de esta Tx. Con esto garantizamos que Transaction siempre quede ya sea con estatus COMPLETED o FAILED
    public void applyDeposit(UUID accountId, BigDecimal amount) {
        AccountEntity accountEntity = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        accountEntity.setBalance(accountEntity.getBalance().add(amount));

        //Lo siguiente no es necesario dado que estamos en una transacción y está managed. Por tanto, automaticamente hibernate se encagará de hacerlo. Esto es conocido como: "Dirty checking"
        //repositoryAccount.save(accountEntity);
    }

}
