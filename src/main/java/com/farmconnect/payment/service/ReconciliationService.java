package com.farmconnect.payment.service;

import com.farmconnect.model.CustomerOrder;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.AllocationRepository;
import com.farmconnect.payment.repository.LedgerRepository;
import com.farmconnect.payment.repository.PaymentRepository;
import com.farmconnect.payment.repository.PayoutRepository;
import com.farmconnect.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Compares what we recorded against itself and against the M-Pesa statement, and lists anything that does not add up.
 * Read-only: it never fixes anything, it only flags it for a person.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    public record Issue(String severity, String code, String reference, String detail) {}
    public record Report(Instant generatedAt, int paymentsChecked, List<Issue> issues) {}
    public record StatementLine(String receipt, long amountKes) {}

    private final PaymentRepository payments;
    private final AllocationRepository allocations;
    private final PayoutRepository payouts;
    private final LedgerRepository ledger;
    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public Report run(int days) {
        Instant now = Instant.now();
        List<Issue> out = new ArrayList<>();
        List<Payment> recent = payments.findByCreatedAtAfter(now.minus(Duration.ofDays(Math.max(1, days))));

        for (Payment p : recent) {
            String ref = "payment #" + p.getId();
            if (p.getStatus() == PaymentStatus.PENDING && p.getCreatedAt().isBefore(now.minus(Duration.ofHours(1))))
                out.add(new Issue("WARN", "STALE_PENDING", ref, "Still PENDING after an hour. Check with Safaricom."));
            if (p.getStatus() == PaymentStatus.NEEDS_REVIEW)
                out.add(new Issue("ERROR", "NEEDS_REVIEW", ref, p.getNote()));
            if (p.getMpesaReceiptNumber() == null) continue;       // no money recorded

            List<PaymentAllocation> allocs = allocations.findByPaymentId(p.getId());
            long live = allocs.stream().filter(a -> a.getStatus() != AllocationStatus.REVERSED).mapToLong(PaymentAllocation::getAmountCents).sum();
            if (live + p.getExcessCents() + p.getRefundedCents() != p.getAmountCents())
                out.add(new Issue("ERROR", "AMOUNT_MISMATCH", ref, "paid " + p.getAmountCents() + " but allocated " + live
                        + " + owed back " + p.getExcessCents() + " + refunded " + p.getRefundedCents() + " (cents)"));

            List<LedgerEntry> lines = ledger.findByPaymentIdOrderByIdAsc(p.getId());
            long dr = 0, cr = 0, collectedNet = 0;
            for (LedgerEntry e : lines) {
                if (e.getDirection() == EntryDirection.DEBIT) dr += e.getAmountCents(); else cr += e.getAmountCents();
                if (LedgerService.COLLECTED.equals(e.getAccountType()))
                    collectedNet += e.getDirection() == EntryDirection.CREDIT ? e.getAmountCents() : -e.getAmountCents();
            }
            if (dr != cr) out.add(new Issue("ERROR", "LEDGER_UNBALANCED", ref, "debits " + dr + " != credits " + cr));
            if (collectedNet != 0) out.add(new Issue("ERROR", "UNALLOCATED_COLLECTION", ref, collectedNet + " cents received but not allocated or marked as owed back"));
            if (lines.isEmpty()) out.add(new Issue("ERROR", "NO_LEDGER", ref, "Receipt " + p.getMpesaReceiptNumber() + " has no ledger entries"));

            if (p.getExcessCents() >= 100 && payouts.findByPaymentId(p.getId()).stream().noneMatch(x -> x.getKind() == PayoutKind.REFUND
                    && x.getStatus() != PayoutStatus.FAILED && x.getStatus() != PayoutStatus.CANCELLED))
                out.add(new Issue("WARN", "REFUND_DUE", ref, "KES " + p.getExcessCents() / 100 + " is owed back to the buyer"));

            for (PaymentAllocation a : allocs) {
                String aref = ref + " / " + a.getBeneficiaryType() + " " + a.getBeneficiaryId();
                List<Payout> ps = payouts.findByAllocationId(a.getId());
                long paid = ps.stream().filter(x -> x.getStatus() == PayoutStatus.SUCCESS).mapToLong(Payout::getAmountCents).sum();
                if (paid != a.getSettledCents())
                    out.add(new Issue("ERROR", "SETTLED_MISMATCH", aref, "allocation says settled " + a.getSettledCents() + " but successful payouts total " + paid));
                if (a.getStatus() == AllocationStatus.SETTLED && a.getSettledCents() != a.getAmountCents())
                    out.add(new Issue("ERROR", "SETTLED_PARTIAL", aref, "marked SETTLED but " + a.getSettledCents() + " of " + a.getAmountCents()));
                if (a.getStatus() == AllocationStatus.SETTLING && ps.stream().noneMatch(x -> x.getStatus() != PayoutStatus.FAILED && x.getStatus() != PayoutStatus.CANCELLED))
                    out.add(new Issue("ERROR", "SETTLING_NO_PAYOUT", aref, "SETTLING but no live payout"));
                if (a.getStatus() == AllocationStatus.ON_HOLD)
                    out.add(new Issue("WARN", "ON_HOLD", aref, a.getHoldReason()));
                if (a.getStatus() == AllocationStatus.ELIGIBLE && p.getCreatedAt().isBefore(now.minus(Duration.ofDays(7))))
                    out.add(new Issue("WARN", "ELIGIBLE_NOT_PAID", aref, "Payable for over a week. Missing verified payout account, or below the minimum?"));
            }
        }

        for (Payout po : payouts.findByStatus(PayoutStatus.UNKNOWN))
            out.add(new Issue("ERROR", "PAYOUT_UNKNOWN", "payout #" + po.getId(), po.getFailureReason()));
        for (Payout po : payouts.findByStatus(PayoutStatus.FAILED))
            out.add(new Issue("ERROR", "PAYOUT_FAILED", "payout #" + po.getId(), po.getFailureReason()));
        for (Payout po : payouts.findByStatusAndLastAttemptAtBefore(PayoutStatus.PROCESSING, now.minus(Duration.ofHours(1))))
            if (!"MANUAL".equals(po.getProviderConversationId()))
                out.add(new Issue("WARN", "PAYOUT_STUCK", "payout #" + po.getId(), "PROCESSING for over an hour"));

        Set<Long> paidOrders = new HashSet<>();
        for (Payment p : payments.findAll()) if (p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.REVERSED) paidOrders.add(p.getOrder().getId());
        for (CustomerOrder o : orders.findByPaymentStatus("PAID"))
            if (!paidOrders.contains(o.getId()))
                out.add(new Issue("INFO", "PAID_WITHOUT_PAYMENT_RECORD", "order #" + o.getId(), "Marked PAID but has no payment record (paid before the allocation system started?). No commission was split."));

        out.sort(Comparator.comparing(Issue::severity));
        log.info("Reconciliation: {} payments checked, {} issues", recent.size(), out.size());
        return new Report(now, recent.size(), out);
    }

    /** Compare a list of receipts+amounts taken from the M-Pesa statement against our records. */
    @Transactional(readOnly = true)
    public List<Issue> compareStatement(List<StatementLine> lines, Instant from, Instant to) {
        List<Issue> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (StatementLine l : lines) {
            String receipt = l.receipt() == null ? "" : l.receipt().trim().toUpperCase();
            if (receipt.isEmpty()) continue;
            if (!seen.add(receipt)) { out.add(new Issue("WARN", "DUPLICATE_LINE", receipt, "Appears twice in the statement")); continue; }
            Optional<Payment> p = payments.findByMpesaReceiptNumber(receipt);
            if (p.isEmpty()) { out.add(new Issue("ERROR", "IN_STATEMENT_NOT_IN_DB", receipt, "KES " + l.amountKes() + " received but not recorded. Confirm or refund it.")); continue; }
            if (p.get().getAmountCents() != l.amountKes() * 100)
                out.add(new Issue("ERROR", "STATEMENT_AMOUNT_DIFFERS", receipt, "statement KES " + l.amountKes() + ", recorded KES " + p.get().getAmountCents() / 100.0));
        }
        if (from != null && to != null)
            for (Payment p : payments.findByCreatedAtAfter(from))
                if (p.getCreatedAt().isBefore(to) && p.getMpesaReceiptNumber() != null && !seen.contains(p.getMpesaReceiptNumber()))
                    out.add(new Issue("ERROR", "IN_DB_NOT_IN_STATEMENT", p.getMpesaReceiptNumber(), "Recorded as paid but missing from the statement"));
        return out;
    }
}
