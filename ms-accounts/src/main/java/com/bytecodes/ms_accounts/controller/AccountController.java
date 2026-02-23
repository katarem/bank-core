package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.response.AccountSummary;
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
