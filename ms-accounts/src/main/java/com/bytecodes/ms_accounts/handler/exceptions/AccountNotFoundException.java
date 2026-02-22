package com.bytecodes.ms_accounts.handler.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("La cuenta " + accountId + " no puede ser encontrada");
    }
}
