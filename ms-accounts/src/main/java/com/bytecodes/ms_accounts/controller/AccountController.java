package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.model.Account;
import com.bytecodes.ms_accounts.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
