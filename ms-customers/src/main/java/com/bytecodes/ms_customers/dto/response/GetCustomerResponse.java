package com.bytecodes.ms_customers.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class GetCustomerResponse {
    private UUID id;
    private String dni;
    private String fullName;
    private String email;
    private String status;
}
