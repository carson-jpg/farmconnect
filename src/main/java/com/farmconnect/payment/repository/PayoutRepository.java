package com.farmconnect.payment.repository;

import com.farmconnect.payment.model.Payout;
import com.farmconnect.payment.model.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payout p where p.id = :id")
    Optional<Payout> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payout p where p.originatorConversationId = :id")
    Optional<Payout> lockByOriginatorConversationId(@Param("id") String id);

    Optional<Payout> findByOriginatorConversationId(String id);
    Optional<Payout> findByProviderConversationId(String id);
    List<Payout> findByAllocationId(Long allocationId);
    List<Payout> findByPaymentId(Long paymentId);
    List<Payout> findByStatus(PayoutStatus status);
    List<Payout> findTop100ByOrderByIdDesc();
    List<Payout> findByStatusAndNextAttemptAtBeforeOrderByIdAsc(PayoutStatus status, Instant before);
    List<Payout> findByStatusAndLastAttemptAtBefore(PayoutStatus status, Instant before);

    /** Atomic claim: only one worker (or server instance) can move a payout from QUEUED to PROCESSING. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Payout p set p.status = com.farmconnect.payment.model.PayoutStatus.PROCESSING, p.lastAttemptAt = :now, " +
           "p.attemptCount = p.attemptCount + 1 where p.id = :id and p.status = com.farmconnect.payment.model.PayoutStatus.QUEUED")
    int claim(@Param("id") Long id, @Param("now") Instant now);
}
