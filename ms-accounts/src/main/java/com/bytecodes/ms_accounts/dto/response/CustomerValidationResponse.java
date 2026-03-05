package com.bytecodes.ms_accounts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValidationResponse {

    private String customerId;
    private boolean exists;
    private boolean isActive;

}
