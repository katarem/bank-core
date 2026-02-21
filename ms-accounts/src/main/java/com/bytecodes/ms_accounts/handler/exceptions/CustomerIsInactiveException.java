package com.bytecodes.ms_accounts.handler.exceptions;

public class CustomerIsInactiveException extends RuntimeException {

    public CustomerIsInactiveException(String message) {
        super(message);
    }

}
