package com.bytecodes.ms_accounts.repository;

import com.bytecodes.ms_accounts.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    long countByCustomerId(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);

}
