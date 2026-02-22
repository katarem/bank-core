package com.bytecodes.ms_accounts.response;

import lombok.Data;

@Data
public class CustomerValidationResponse {

    private String customerId;
    private boolean exists;
    private boolean isActive;

}
