package com.bytecodes.ms_accounts.model;

import lombok.Data;

import java.util.UUID;

@Data
public class AuthPrincipal {
    private String username;
    private UUID customerId;
}
