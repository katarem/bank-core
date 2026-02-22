package com.bytecodes.ms_accounts.handler.exceptions;

public class CreateAccountLimitExceededException extends RuntimeException {

    public CreateAccountLimitExceededException(String message) {
        super(message);
    }

}
