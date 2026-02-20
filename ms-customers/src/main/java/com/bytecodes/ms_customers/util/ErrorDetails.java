package com.bytecodes.ms_customers.util;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorDetails {
    private String code;
    private String message;
    private Instant timestamp;
}
