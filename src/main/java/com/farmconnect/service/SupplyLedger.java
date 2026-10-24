package com.farmconnect.service;

import com.farmconnect.model.SupplyLedgerEntry;
import com.farmconnect.payment.model.EntryDirection;
import com.farmconnect.repository.SupplyLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes the supply ledger. Always joins the caller's transaction, so money state and ledger commit together. */
@Component
@RequiredArgsConstructor
public class SupplyLedger {
    public static String payableCompany(Long companyId) { return "PAYABLE:COMPANY:" + companyId; }
    public static String payableBuyer(Long userId) { return "PAYABLE:BUYER:" + userId; }
    public static final String PAYABLE_COUNTY = "PAYABLE:COUNTY:SUPPLY";

    private final SupplyLedgerRepository repo;

    /** One movement = one DEBIT line + one CREDIT line of the same amount. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void move(Long supplyPaymentId, String entryType, String debit, String credit, long cents, String reference) {
        if (cents <= 0) return;
        repo.save(line(supplyPaymentId, entryType, debit, EntryDirection.DEBIT, cents, reference));
        repo.save(line(supplyPaymentId, entryType, credit, EntryDirection.CREDIT, cents, reference));
    }

    private static SupplyLedgerEntry line(Long id, String type, String account, EntryDirection d, long cents, String ref) {
        SupplyLedgerEntry e = new SupplyLedgerEntry();
        e.setSupplyPaymentId(id);
        e.setEntryType(type);
        e.setAccountType(account);
        e.setDirection(d);
        e.setAmountCents(cents);
        e.setReference(ref == null ? null : ref.length() > 200 ? ref.substring(0, 200) : ref);
        return e;
    }
}
