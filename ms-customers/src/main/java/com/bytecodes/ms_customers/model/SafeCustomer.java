package com.bytecodes.ms_customers.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class SafeCustomer {
    private UUID id;
    private String dni;
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private CustomerStatus status;
    private UserRole role;
    private Instant createdAt;
}
