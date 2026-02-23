package com.bytecodes.ms_accounts.handler.exceptions;

public class CreateAccountLimitExceededException extends RuntimeException {

    public CreateAccountLimitExceededException() {
        super("El cliente ha alcanzado el máximo de cuentas permitidas");
    }

}
