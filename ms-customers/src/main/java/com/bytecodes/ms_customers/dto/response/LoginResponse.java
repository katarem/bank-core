package com.bytecodes.ms_customers.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private String customerId;

}
