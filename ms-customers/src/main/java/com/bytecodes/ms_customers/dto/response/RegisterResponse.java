package com.bytecodes.ms_customers.dto.response;

import com.bytecodes.ms_customers.model.CustomerStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RegisterResponse {
    private UUID id;
    private String dni;
    private String fullName;
    private String email;
    private CustomerStatus status;
    private Instant createdAt;
}
