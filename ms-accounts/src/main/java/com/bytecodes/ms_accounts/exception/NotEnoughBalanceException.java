package com.bytecodes.ms_accounts.exception;

public class NotEnoughBalanceException extends RuntimeException {
    public NotEnoughBalanceException(){
        super("No hay suficiente saldo para la transacción");
    }
}
