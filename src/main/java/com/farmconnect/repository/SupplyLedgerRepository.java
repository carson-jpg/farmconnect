package com.farmconnect.repository;

import com.farmconnect.model.SupplyLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SupplyLedgerRepository extends JpaRepository<SupplyLedgerEntry, Long> {
    List<SupplyLedgerEntry> findBySupplyPaymentIdOrderByIdAsc(Long paymentId);

    /** Payments whose debits and credits differ. Must always be empty. */
    @Query("select e.supplyPaymentId from SupplyLedgerEntry e group by e.supplyPaymentId "
            + "having sum(case when e.direction = com.farmconnect.payment.model.EntryDirection.DEBIT then e.amountCents else 0 end) "
            + "<> sum(case when e.direction = com.farmconnect.payment.model.EntryDirection.CREDIT then e.amountCents else 0 end)")
    List<Long> unbalancedPayments();
}
