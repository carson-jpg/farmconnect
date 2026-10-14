package com.farmconnect.payment.repository;

import com.farmconnect.payment.model.EntryDirection;
import com.farmconnect.payment.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByPaymentIdOrderByIdAsc(Long paymentId);

    @Query("select coalesce(sum(e.amountCents),0) from LedgerEntry e where e.paymentId = :pid and e.direction = :d")
    long sum(@Param("pid") Long paymentId, @Param("d") EntryDirection d);

    @Query("select coalesce(sum(e.amountCents),0) from LedgerEntry e where e.accountType = :acc and e.direction = :d")
    long sumAccount(@Param("acc") String account, @Param("d") EntryDirection d);
}
