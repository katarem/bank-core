package com.bytecodes.ms_accounts.handler.exceptions;

public class CustomerIsInactiveException extends RuntimeException {

    public CustomerIsInactiveException() {
        super("El cliente no está activo. No es posible crear la cuenta.");
    }

}
