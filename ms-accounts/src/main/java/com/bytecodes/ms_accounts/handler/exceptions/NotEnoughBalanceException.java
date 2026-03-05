package com.bytecodes.ms_accounts.handler.exceptions;

public class NotEnoughBalanceException extends RuntimeException {
    public NotEnoughBalanceException(){
        super("No hay suficiente saldo para la transacción");
    }
}
