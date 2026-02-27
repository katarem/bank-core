package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.response.AccountSummary;
<<<<<<< HEAD
import com.bytecodes.ms_accounts.service.AccountBalanceService;
=======
>>>>>>> 713e688cab228ea9621479719556596cb7f153dc
import com.bytecodes.ms_accounts.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;
    private final AccountBalanceService serviceAccountBalance;

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<DepositResponse> deposit(@PathVariable UUID accountId,
                                                   @RequestBody @Valid DepositRequest request,
                                                   @RequestHeader(value = "Authorization") String token) {
        return ResponseEntity.ok(serviceAccountBalance.deposit(accountId,
                                                 request,
                                                 token.replace("Bearer ", "")));
    }


    @PostMapping
    public ResponseEntity<Account> registerAccount(@RequestBody @Valid Account account,
                                                   @RequestHeader(value = "Authorization") String token) {
        Account accountCreated = service.registerAccount(account, token.replace("Bearer ", ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(accountCreated);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummary>> getMyAccounts(@RequestHeader(value = "Authorization") String token) {
        List<AccountSummary> accounts = service.getMyAccounts(token.replace("Bearer ", ""));
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable UUID accountId,
                                                  @RequestHeader(value = "Authorization") String token) {
        Account account = service.getAccount(accountId, token.replace("Bearer ", ""));
        return ResponseEntity.ok(account);
    }
}
