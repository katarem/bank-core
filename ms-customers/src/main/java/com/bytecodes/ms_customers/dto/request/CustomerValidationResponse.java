package com.bytecodes.ms_customers.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class CustomerValidationResponse {
    
    private UUID customerId;
    private boolean exists;
    private boolean isActive;
}
