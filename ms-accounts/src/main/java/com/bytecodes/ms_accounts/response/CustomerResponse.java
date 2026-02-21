package com.bytecodes.ms_accounts.response;

import lombok.Data;

@Data
public class CustomerResponse {
    private String id;
    private String dni;
    private String fullName;
    private String email;
    private String status;
}
