package com.bytecodes.ms_accounts.handler.exceptions;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Saldo insuficiente para realizar el retiro");
    }
}
