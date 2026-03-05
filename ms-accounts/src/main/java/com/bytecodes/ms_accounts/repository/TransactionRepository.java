package com.bytecodes.ms_accounts.repository;

import com.bytecodes.ms_accounts.entity.TransactionEntity;
import com.bytecodes.ms_accounts.model.TransactionStatus;
import com.bytecodes.ms_accounts.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("""
	    SELECT COALESCE(SUM(t.amount), 0)
	    FROM TransactionEntity t
	    WHERE t.accountId = :accountId
	      AND t.type = :type
	      AND t.status = :status
	      AND t.createdAt >= :start
	      AND t.createdAt < :end
	    """)
    BigDecimal sumAmountByAccountAndTypeAndStatusBetween(
	    @Param("accountId") UUID accountId,
	    @Param("type") TransactionType type,
	    @Param("status") TransactionStatus status,
	    @Param("start") Instant start,
	    @Param("end") Instant end
    );
}
