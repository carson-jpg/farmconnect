package com.farmconnect.payment.service;

import com.farmconnect.payment.model.BeneficiaryType;
import com.farmconnect.payment.model.EntryDirection;
import com.farmconnect.payment.model.LedgerEntry;
import com.farmconnect.payment.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes the append-only ledger. Always joins the caller's database transaction, so money state and ledger commit together. */
@Service
@RequiredArgsConstructor
public class LedgerService {
    public static final String CLEARING = "MPESA_CLEARING";                 // cash in our M-Pesa account
    public static final String COLLECTED = "COLLECTIONS_RECEIVED";          // received, not yet allocated
    public static final String PLATFORM_REVENUE = "PLATFORM_REVENUE";       // FarmConnect's retained share

    private final LedgerRepository repo;

    public static String payable(BeneficiaryType t, String id) {
        return t == BeneficiaryType.PLATFORM ? "PAYABLE:PLATFORM" : "PAYABLE:" + t + ":" + id;
    }

    /** One movement = one DEBIT line + one CREDIT line of the same amount. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void move(Long paymentId, Long allocationId, String entryType, String debitAccount, String creditAccount,
                     long cents, String reference) {
        if (cents <= 0) return;
        repo.save(line(paymentId, allocationId, entryType, debitAccount, EntryDirection.DEBIT, cents, reference));
        repo.save(line(paymentId, allocationId, entryType, creditAccount, EntryDirection.CREDIT, cents, reference));
    }

    private LedgerEntry line(Long paymentId, Long allocationId, String type, String account, EntryDirection d, long cents, String ref) {
        LedgerEntry e = new LedgerEntry();
        e.setPaymentId(paymentId);
        e.setAllocationId(allocationId);
        e.setEntryType(type);
        e.setAccountType(account);
        e.setDirection(d);
        e.setAmountCents(cents);
        e.setReference(ref == null ? null : ref.length() > 200 ? ref.substring(0, 200) : ref);
        return e;
    }
}
