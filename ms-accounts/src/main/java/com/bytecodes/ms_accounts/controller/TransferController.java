package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.dto.request.CreateTransferRequest;
import com.bytecodes.ms_accounts.dto.response.CreateTransferResponse;
import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.service.AccountBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AccountBalanceService service;

    @PostMapping
    public ResponseEntity<CreateTransferResponse> createTransfer(@RequestBody @Valid CreateTransferRequest request,
                                                                 @AuthenticationPrincipal AuthPrincipal authentication) {
        var response = service.createTransfer(request, authentication);
        return ResponseEntity.ok(response);
    }

}
