package com.bytecodes.ms_accounts.handler.exceptions;

public class NotOwnAccountException extends RuntimeException {
    public NotOwnAccountException() {
        super("No tienes acceso a esta cuenta.");
    }
}
