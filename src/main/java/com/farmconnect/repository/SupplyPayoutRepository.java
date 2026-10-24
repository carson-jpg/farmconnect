package com.farmconnect.repository;

import com.farmconnect.model.SupplyPayout;
import com.farmconnect.model.SupplyPayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SupplyPayoutRepository extends JpaRepository<SupplyPayout, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplyPayout p where p.id = :id")
    Optional<SupplyPayout> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplyPayout p where p.originatorConversationId = :id")
    Optional<SupplyPayout> lockByOriginatorConversationId(@Param("id") String id);

    Optional<SupplyPayout> findByProviderConversationId(String id);
    List<SupplyPayout> findByStatusAndNextAttemptAtBeforeOrderByIdAsc(SupplyPayoutStatus status, Instant before);
    List<SupplyPayout> findByStatusAndLastAttemptAtBefore(SupplyPayoutStatus status, Instant before);
    List<SupplyPayout> findByStatusOrderByIdDesc(SupplyPayoutStatus status);
    List<SupplyPayout> findByCompanyIdOrderByIdDesc(Long companyId);
    List<SupplyPayout> findTop100ByOrderByIdDesc();

    /** Atomic: only one worker can move a payout from QUEUED to PROCESSING. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SupplyPayout p set p.status = com.farmconnect.model.SupplyPayoutStatus.PROCESSING, "
            + "p.attemptCount = p.attemptCount + 1, p.lastAttemptAt = :now "
            + "where p.id = :id and p.status = com.farmconnect.model.SupplyPayoutStatus.QUEUED")
    int claim(@Param("id") Long id, @Param("now") Instant now);
}
