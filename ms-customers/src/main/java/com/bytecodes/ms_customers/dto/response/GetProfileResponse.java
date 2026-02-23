package com.bytecodes.ms_customers.dto.response;

import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.model.UserRole;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class GetProfileResponse {
    private UUID id;
    private String dni;
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private String phone;
    private CustomerStatus status;
    private UserRole role;
    private Instant createdAt;
}
