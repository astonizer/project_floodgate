package com.example.floodgate.repository;


import com.example.floodgate.models.entity.Payout;
import com.example.floodgate.models.enums.PayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    // -------------------------------------------------------------------------
    // Idempotency lookups
    // -------------------------------------------------------------------------

    /** Plain read — called before acquiring any lock (fast path). */
    Optional<Payout> findByPayoutId(String payoutId);

    /**
     * Pessimistic write lock — fallback when Redis is unavailable.
     * Backed by {@code idx_payouts_payout_id}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payout p WHERE p.payoutId = :payoutId")
    Optional<Payout> findByPayoutIdForUpdate(@Param("payoutId") String payoutId);

    // -------------------------------------------------------------------------
    // NEW_BENEFICIARY rule
    // Uses idx_payouts_new_beneficiary: (source_account, beneficiary_account, status)
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if a prior clean payout exists from {@code sourceAccount}
     * to {@code beneficiaryAccount} with one of the given statuses.
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM Payout p
        WHERE p.sourceAccount      = :sourceAccount
          AND p.beneficiaryAccount = :beneficiaryAccount
          AND p.status IN :statuses
        """)
    boolean existsCleanPayout(
            @Param("sourceAccount")      String              sourceAccount,
            @Param("beneficiaryAccount") String              beneficiaryAccount,
            @Param("statuses") List<PayoutStatus> statuses
    );

    // -------------------------------------------------------------------------
    // GET /api/payouts?status=  (paginated, newest-first)
    // Uses idx_payouts_status_created_all: (status, created_at DESC)
    // -------------------------------------------------------------------------

    Page<Payout> findByStatusOrderByCreatedAtDesc(PayoutStatus status, Pageable pageable);
}

