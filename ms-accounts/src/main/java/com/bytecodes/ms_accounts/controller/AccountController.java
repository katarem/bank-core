package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.dto.request.RegisterAccountRequest;
import com.bytecodes.ms_accounts.dto.response.AccountSummary;
import com.bytecodes.ms_accounts.dto.response.DepositResponse;
import com.bytecodes.ms_accounts.dto.request.DepositRequest;
import com.bytecodes.ms_accounts.dto.response.GetAccountResponse;
import com.bytecodes.ms_accounts.dto.response.RegisterAccountResponse;
import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.service.AccountBalanceService;
import com.bytecodes.ms_accounts.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                                                   @AuthenticationPrincipal AuthPrincipal auth) {
        return ResponseEntity.ok(serviceAccountBalance.deposit(accountId,
                                                 request,
                                                 auth));
    }


    @PostMapping
    public ResponseEntity<RegisterAccountResponse> registerAccount(@RequestBody @Valid RegisterAccountRequest request,
                                                                   @AuthenticationPrincipal AuthPrincipal auth) {
        RegisterAccountResponse accountCreated = service.registerAccount(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountCreated);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummary>> getMyAccounts(@AuthenticationPrincipal AuthPrincipal auth) {
        List<AccountSummary> accounts = service.getMyAccounts(auth);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<GetAccountResponse> getAccountById(@PathVariable UUID accountId,
                                                  @AuthenticationPrincipal AuthPrincipal auth) {
        GetAccountResponse account = service.getAccount(accountId, auth);
        return ResponseEntity.ok(account);
    }
}
