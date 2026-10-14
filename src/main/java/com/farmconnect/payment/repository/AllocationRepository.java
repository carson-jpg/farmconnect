package com.farmconnect.payment.repository;

import com.farmconnect.payment.model.AllocationStatus;
import com.farmconnect.payment.model.BeneficiaryType;
import com.farmconnect.payment.model.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByPaymentId(Long paymentId);
    List<PaymentAllocation> findByStatus(AllocationStatus status);
    List<PaymentAllocation> findByBeneficiaryTypeAndBeneficiaryIdOrderByIdDesc(BeneficiaryType t, String id);

    /** Allocations still waiting to become payable, together with the order they belong to. */
    @Query("select a from PaymentAllocation a join fetch a.payment p join fetch p.order o where a.status = :status")
    List<PaymentAllocation> findWithOrder(@Param("status") AllocationStatus status);

    @Query("select coalesce(sum(a.amountCents),0) from PaymentAllocation a where a.beneficiaryType = :t and a.beneficiaryId = :id and a.status in :statuses")
    long sumFor(@Param("t") BeneficiaryType t, @Param("id") String id, @Param("statuses") List<AllocationStatus> statuses);

    @Query("select coalesce(sum(a.amountCents),0) from PaymentAllocation a where a.beneficiaryType = :t and a.status in :statuses")
    long sumByType(@Param("t") BeneficiaryType t, @Param("statuses") List<AllocationStatus> statuses);
}
