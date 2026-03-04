package com.bytecodes.ms_accounts.dto.response;

import com.bytecodes.ms_accounts.model.TransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CreateTransferResponse {
    private UUID transferId;
    private TransferStatus status;
    private String sourceAccount;
    private String destinationAccount;
    private String beneficiaryName;
    private BigDecimal amount;
    private String concept;
    private BigDecimal fee;
    private BigDecimal totalDebited;
    private Instant timestamp;
}
