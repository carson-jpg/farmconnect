package com.farmconnect.payment.repository;

import com.farmconnect.payment.model.Payment;
import com.farmconnect.payment.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    /** Row-locked, so two callbacks for the same payment are processed one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.checkoutRequestId = :id")
    Optional<Payment> lockByCheckoutRequestId(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> lockById(@Param("id") Long id);

    Optional<Payment> findByMpesaReceiptNumber(String receipt);
    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant before);
    List<Payment> findByStatusOrderByIdDesc(PaymentStatus status);
    List<Payment> findTop100ByOrderByIdDesc();
    List<Payment> findByCreatedAtAfter(Instant after);

    @Query("select coalesce(sum(p.amountCents),0) from Payment p where p.status = :s and p.mpesaReceiptNumber is not null")
    long sumAmount(@Param("s") PaymentStatus s);
}
