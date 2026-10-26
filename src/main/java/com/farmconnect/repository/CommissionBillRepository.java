package com.farmconnect.repository;

import com.farmconnect.model.CommissionBill;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommissionBillRepository extends JpaRepository<CommissionBill, Long> {
    List<CommissionBill> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<CommissionBill> findTop300ByOrderByCreatedAtDesc();
    List<CommissionBill> findByStatusInOrderByCreatedAtAsc(Collection<String> statuses);
    Optional<CommissionBill> findByCashSaleId(Long cashSaleId);
    List<CommissionBill> findByFarmerIdAndStatusIn(Long farmerId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CommissionBill b where b.id = :id")
    Optional<CommissionBill> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CommissionBill b where b.checkoutRequestId = :id")
    Optional<CommissionBill> lockByCheckoutRequestId(@Param("id") String id);
}
