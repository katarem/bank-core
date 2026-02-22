package com.bytecodes.ms_accounts.model;

import lombok.Getter;

/**
 * Representa el payload que puede contener el JWT
 */
@Getter
public enum JwtClaim {

    CUSTOMER_ID("customerId", String.class),
    ROLE("role", String.class);

    private final String claimName;
    private final Class<?> type;

    JwtClaim(String claimName, Class<?> type) {
        this.claimName = claimName;
        this.type = type;
    }

}
