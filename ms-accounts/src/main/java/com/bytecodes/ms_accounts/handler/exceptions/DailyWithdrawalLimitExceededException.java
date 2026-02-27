package com.bytecodes.ms_accounts.handler.exceptions;

public class DailyWithdrawalLimitExceededException extends RuntimeException {

    public DailyWithdrawalLimitExceededException() {
        super("Se excedió el límite diario de retiro");
    }
}
