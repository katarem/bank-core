package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<DepositResponse> deposit(@PathVariable UUID accountId,
                                                   @RequestBody @Valid DepositRequest request,
                                                   @RequestHeader(value = "Authorization") String token) {
        return ResponseEntity.ok(service.deposit(accountId,
                                                 request,
                                                 token.replace("Bearer ", "")));
    }


    @PostMapping
    public ResponseEntity<Account> registerAccount(@RequestBody @Valid Account account,
                                                   @RequestHeader(value = "Authorization") String token) {
        Account accountCreated = service.registerAccount(account, token.replace("Bearer ", ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(accountCreated);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable UUID accountId,
                                                  @RequestHeader(value = "Authorization") String token) {
        Account account = service.getAccount(accountId, token.replace("Bearer ", ""));
        return ResponseEntity.ok(account);
    }
}
