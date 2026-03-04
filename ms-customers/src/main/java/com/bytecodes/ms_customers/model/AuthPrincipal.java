package com.bytecodes.ms_customers.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthPrincipal {
    private String username;
    private String customerId;
}
