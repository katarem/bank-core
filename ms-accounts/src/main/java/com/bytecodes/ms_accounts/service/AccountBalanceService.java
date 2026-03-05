package com.bytecodes.ms_accounts.service;

import com.bytecodes.ms_accounts.client.CustomerClient;
import com.bytecodes.ms_accounts.dto.request.CreateTransferRequest;
import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.response.CreateTransferResponse;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.entity.AccountEntity;
import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.handler.exceptions.NotEnoughBalanceException;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.DailyWithdrawalLimitExceededException;
import com.bytecodes.ms_accounts.handler.exceptions.NotOwnAccountException;
import com.bytecodes.ms_accounts.mapper.TransactionMapper;
import com.bytecodes.ms_accounts.model.*;
import com.bytecodes.ms_accounts.repository.AccountRepository;
import com.bytecodes.ms_accounts.repository.TransactionRepository;
import com.bytecodes.ms_accounts.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Se crea un nuevo service para administrar el balance de manera independiente
 * y en caso que falle algo se maneje en una nueva transacción. Con esto garantizamos
 * que la entidad TransactionEntity pueda manejar históricos con FAILED y COMPLETED.
 * <p>
 * Esto toca en un nuevo service dado que Spring maneja @Transactional con proxies:
 * Cuando un método del mismo bean llama a otro método del mismo bean:
 * -NO pasa por el proxy
 * -NO se crea nueva transacción
 * -REQUIRES_NEW es ignorado
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceService {

    private final TransactionRepository repositoryTransaction;
    private final AccountRepository repositoryAccount;
    private final TransactionMapper mapperTransaction;
    private final CustomerClient client;
    private final JwtUtil jwtUtil;

    @Value("${bank.daily-withdrawal-limit:1000}")
    private BigDecimal defaultDailyWithdrawalLimit;

    @Value("${bank.fee:0}")
    private BigDecimal FEE;

    public DepositResponse deposit(final UUID accountId, final DepositRequest request, final AuthPrincipal auth) {
        AccountEntity account = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        checkOwnerAccount(auth.getCustomerId(), account);

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

    public DepositResponse withdraw(final UUID accountId, final DepositRequest request, final AuthPrincipal auth) {
        AccountEntity account = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        checkOwnerAccount(auth.getCustomerId(), account);

        BigDecimal amount = request.getAmount();
        BigDecimal dailyLimit = account.getDailyWithdrawalLimit() == null
            ? defaultDailyWithdrawalLimit
                : account.getDailyWithdrawalLimit();

        if (amount.compareTo(dailyLimit) > 0) {
            throw new DailyWithdrawalLimitExceededException();
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new NotEnoughBalanceException();
        }

        BigDecimal balanceBefore = account.getBalance();

        TransactionEntity transactionEntity = TransactionEntity.builder()
                .accountId(accountId)
            .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.subtract(amount))
                .concept(request.getDescription())
                .status(TransactionStatus.PENDING)
                .build();

        repositoryTransaction.save(transactionEntity);

        try {
            AccountEntity accountUpdated = this.applyWithdrawal(accountId, amount);
            transactionEntity.setStatus(TransactionStatus.COMPLETED);
            transactionEntity.setBalanceAfter(accountUpdated.getBalance());
        } catch (Exception e) {
            transactionEntity.setBalanceAfter(balanceBefore);
            transactionEntity.setStatus(TransactionStatus.FAILED);
        }

        repositoryTransaction.save(transactionEntity);

        return mapperTransaction.toDepositResponse(mapperTransaction.toModel(transactionEntity));
    }

    public CreateTransferResponse createTransfer(final CreateTransferRequest request, final AuthPrincipal authentication) {

        // Retrieve source account
        AccountEntity sourceAccount = repositoryAccount.findById(request.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getSourceAccountId().toString()));

        // use example for easy filtering
        AccountEntity example = new AccountEntity();
        example.setAccountNumber(request.getDestinationAccountNumber());
        AccountEntity destinationAccount = repositoryAccount.findOne(Example.of(example))
                .orElseThrow(() -> new AccountNotFoundException(request.getDestinationAccountNumber()));

        // if the destination user is other, we have to check if it's active
        if (!destinationAccount.getCustomerId().equals(authentication.getCustomerId())) {
            var response = client.validateCustomer(authentication.getCustomerId());
            if (!response.isExists() || !response.isActive())
                throw new UsernameNotFoundException("Destination customer does not exist");
        }

        // check if the origin customer is active
        var response = client.validateCustomer(authentication.getCustomerId());
        if (!response.isExists() || !response.isActive())
            throw new UsernameNotFoundException("Origin customer does not exist");

        // check if origin customer is owner of origin account
        checkOwnerAccount(authentication.getCustomerId(), sourceAccount);

        // process the operation
        var sourceBalanceAfter = sourceAccount.getBalance().subtract(request.getAmount().add(FEE));
        var destinationBalanceAfter = destinationAccount.getBalance().add(request.getAmount());

        // retrieve customer info (counter party name)
        var sourceCustomer = client.getCustomer(sourceAccount.getCustomerId());
        var destinationCustomer = authentication.getCustomerId().equals(destinationAccount.getCustomerId()) ? sourceCustomer : client.getCustomer(destinationAccount.getCustomerId());

        // generate 2 transactions, the money out and the money in
        TransactionEntity transactionRemoveMoney = TransactionEntity.builder()
                .accountId(request.getSourceAccountId())
                .balanceBefore(sourceAccount.getBalance())
                .balanceAfter(sourceBalanceAfter)
                .amount(request.getAmount().negate())
                .counterpartyAccountNumber(sourceAccount.getAccountNumber())
                .counterpartyName(sourceCustomer.getFullName())
                .concept(request.getConcept())
                .status(TransactionStatus.PENDING)
                .type(TransactionType.TRANSFER_OUT)
                .build();

        TransactionEntity transactionAddMoney = TransactionEntity.builder()
                .accountId(destinationAccount.getId())
                .balanceBefore(destinationAccount.getBalance())
                .balanceAfter(destinationBalanceAfter)
                .amount(request.getAmount())
                .counterpartyAccountNumber(destinationAccount.getAccountNumber())
                .counterpartyName(destinationCustomer.getFullName())
                .status(TransactionStatus.PENDING)
                .type(TransactionType.TRANSFER_IN)
                .build();

        List<TransactionEntity> transactions = repositoryTransaction.saveAll(List.of(transactionRemoveMoney, transactionAddMoney));

        // check if source account has enough balance for the substraction

        try {
            if (sourceAccount.getBalance().compareTo(request.getAmount().add(FEE)) < 0) {
                throw new NotEnoughBalanceException();
            }
            transactions.forEach(transaction -> transaction.setStatus(TransactionStatus.COMPLETED));
        } catch (Exception e) {
            log.error("No hubo cantidad suficiente para la transferencia");
            transactions.forEach(transaction -> transaction.setStatus(TransactionStatus.FAILED));
        }

        repositoryTransaction.saveAll(transactions);

        TransferStatus transferStatus = transactions.getFirst().getStatus().equals(TransactionStatus.COMPLETED)
                ? TransferStatus.COMPLETED
                : TransferStatus.FAILED;

        BigDecimal totalDebited = transferStatus.equals(TransferStatus.COMPLETED)
                ? request.getAmount().add(FEE)
                : BigDecimal.ZERO;

        return CreateTransferResponse.builder()
                .transferId(transactionRemoveMoney.getId())
                .fee(FEE)
                .beneficiaryName(destinationCustomer.getFullName())
                .sourceAccount(sourceAccount.getAccountNumber())
                .destinationAccount(destinationAccount.getAccountNumber())
                .concept(request.getConcept())
                .amount(request.getAmount())
                .status(transferStatus)
                .timestamp(Instant.now())
                .totalDebited(totalDebited)
                .build();
    }

    /**
     * Verifica que la cuenta proporcionada pertenezca al cliente autenticado.
     *
     * @param customerId Token JWT del cual se extrae el identificador del cliente.
     * @param account    Entidad de cuenta que se desea validar.
     */
    private void checkOwnerAccount(UUID customerId, AccountEntity account) {
        if (!customerId.equals(account.getCustomerId())) {
            throw new NotOwnAccountException();
        }
    }

    /**
     * Se encarga de realizar un deposito en un "hilo"/transacción nueva. En caso de fallo lanzará una
     * excepción.
     *
     * @param accountId Identificador de la cuenta
     * @param amount    Monto a depositar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
//Se ejecuta en una nueva transacción, en caso de que falle, solo se hará rollback de esta Tx. Con esto garantizamos que Transaction siempre quede ya sea con estatus COMPLETED o FAILED
    private AccountEntity applyDeposit(UUID accountId, BigDecimal amount) {
        AccountEntity accountEntity = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        accountEntity.setBalance(accountEntity.getBalance().add(amount));

        //Lo siguiente no es necesario dado que estamos en una transacción y está managed. Por tanto, automáticamente hibérnate se encagará de hacerlo. Esto es conocido como: "Dirty checking"
        //repositoryAccount.save(accountEntity);

        return accountEntity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private AccountEntity applyWithdrawal(UUID accountId, BigDecimal amount) {
        AccountEntity accountEntity = repositoryAccount.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        accountEntity.setBalance(accountEntity.getBalance().subtract(amount));

        return accountEntity;
    }

}
