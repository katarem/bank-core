package com.bytecodes.ms_accounts.entity;

import com.bytecodes.ms_accounts.model.TransactionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transfer")
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID sourceAccountId;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private String concept;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private LocalDate scheduledDate;
    private Instant executedAt;
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
