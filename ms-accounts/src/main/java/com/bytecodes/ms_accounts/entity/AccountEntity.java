package com.bytecodes.ms_accounts.entity;

import com.bytecodes.ms_accounts.model.AccountStatus;
import com.bytecodes.ms_accounts.model.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@EqualsAndHashCode
@Table(name = "account")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true)
    private String accountNumber;
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    private String currency;
    private BigDecimal balance;
    private String alias;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    private BigDecimal dailyWithdrawalLimit;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

}
