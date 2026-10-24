package com.farmconnect.repository;

import com.farmconnect.model.SupplyPayment;
import com.farmconnect.model.SupplyPaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SupplyPaymentRepository extends JpaRepository<SupplyPayment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplyPayment p where p.checkoutRequestId = :id")
    Optional<SupplyPayment> lockByCheckoutRequestId(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplyPayment p where p.id = :id")
    Optional<SupplyPayment> lockById(@Param("id") Long id);

    Optional<SupplyPayment> findByMpesaReceiptNumber(String receipt);
    List<SupplyPayment> findByStatusAndCreatedAtBefore(SupplyPaymentStatus status, Instant before);
    List<SupplyPayment> findByStatusOrderByIdDesc(SupplyPaymentStatus status);
    List<SupplyPayment> findTop100ByOrderByIdDesc();
    boolean existsByOrderIdAndStatusAndCreatedAtAfter(Long orderId, SupplyPaymentStatus status, Instant after);
    boolean existsByOrderIdAndStatus(Long orderId, SupplyPaymentStatus status);
}
