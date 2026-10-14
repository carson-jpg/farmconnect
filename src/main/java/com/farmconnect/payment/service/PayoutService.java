package com.farmconnect.payment.service;

import com.farmconnect.model.CustomerOrder;
import com.farmconnect.model.OrderStatus;
import com.farmconnect.model.User;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.AllocationRepository;
import com.farmconnect.payment.repository.PayoutRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.service.MpesaService;
import com.farmconnect.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Actual money going out. The rules that prevent paying twice:
 *  1. A payout is claimed QUEUED -> PROCESSING with one atomic UPDATE and committed BEFORE anything is sent.
 *  2. Every attempt has its own OriginatorConversationID that Safaricom echoes back and rejects if repeated.
 *  3. If we cannot tell whether Safaricom got the request (timeout, 5xx), the payout becomes UNKNOWN and is NEVER
 *     retried automatically. A person checks the M-Pesa statement first.
 *  4. Allocations only become SETTLED from Safaricom's result, not because the request was accepted.
 */
@Service
@RequiredArgsConstructor
public class PayoutService {
    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);
    private static final String MANUAL = "MANUAL";

    private final PayoutRepository payouts;
    private final AllocationRepository allocations;
    private final BeneficiaryService accounts;
    private final LedgerService ledger;
    private final MpesaService mpesa;
    private final PaymentPolicy policy;
    private final UserRepository users;
    private final NotificationService notifier;
    private final TransactionTemplate tx;

    // ------------------------------------------------------------------------------------------ eligibility

    /** RECORDED -> ELIGIBLE once the order is DELIVERED and the dispute window (payments.hold-hours) has passed. */
    public int promoteEligible() {
        Integer n = tx.execute(s -> {
            int count = 0;
            Instant cutoff = Instant.now().minus(Duration.ofHours(policy.getHoldHours()));
            for (PaymentAllocation a : allocations.findWithOrder(AllocationStatus.RECORDED)) {
                CustomerOrder o = a.getPayment().getOrder();
                if (o.getStatus() != OrderStatus.DELIVERED) continue;
                Instant at = o.getDeliveredAt() != null ? o.getDeliveredAt() : o.getUpdatedAt();
                if (at == null || at.isAfter(cutoff)) continue;
                a.setStatus(AllocationStatus.ELIGIBLE);
                count++;
            }
            return count;
        });
        return n == null ? 0 : n;
    }

    /** ELIGIBLE allocations get a QUEUED payout, but only if a verified payout account exists. */
    public int enqueuePayouts() {
        List<Long> ids = tx.execute(s -> allocations.findByStatus(AllocationStatus.ELIGIBLE).stream().map(PaymentAllocation::getId).toList());
        int made = 0;
        if (ids == null) return 0;
        for (Long id : ids) {
            Integer r = tx.execute(s -> enqueueOne(id));
            if (r != null) made += r;
        }
        return made;
    }

    private int enqueueOne(Long id) {
        PaymentAllocation a = allocations.findById(id).orElse(null);
        if (a == null || a.getStatus() != AllocationStatus.ELIGIBLE) return 0;
        long remaining = a.getAmountCents() - a.getSettledCents();
        if (remaining <= 0) { a.setStatus(AllocationStatus.SETTLED); return 0; }
        boolean live = payouts.findByAllocationId(id).stream()
                .anyMatch(p -> p.getStatus() != PayoutStatus.FAILED && p.getStatus() != PayoutStatus.CANCELLED);
        if (live) return 0;

        Optional<BeneficiaryAccount> dest = destination(a);
        if (dest.isEmpty()) return 0;                       // waits for a verified account
        BeneficiaryAccount acct = dest.get();

        if (acct.getAccountType() == AccountType.RETAINED) { retain(a, acct, remaining); return 1; }
        if (acct.getAccountType() != AccountType.BANK && remaining < policy.getMinPayoutKes() * 100) return 0;   // below Safaricom's minimum

        Payout p = new Payout();
        p.setKind(PayoutKind.ALLOCATION);
        p.setAllocation(a);
        p.setPayment(a.getPayment());
        p.setBeneficiaryType(a.getBeneficiaryType());
        p.setBeneficiaryId(a.getBeneficiaryId());
        p.setAccountType(acct.getAccountType());
        p.setDestinationAccount(acct.getAccountReference());
        p.setDestinationReference(acct.getAccountNumber());
        p.setAmountCents(remaining);
        p.setStatus(PayoutStatus.QUEUED);
        payouts.save(p);
        a.setStatus(AllocationStatus.SETTLING);
        return 1;
    }

    private Optional<BeneficiaryAccount> destination(PaymentAllocation a) {
        if (a.getBeneficiaryType() == BeneficiaryType.FARMER) {
            Optional<User> u = users.findById(Long.parseLong(a.getBeneficiaryId()));
            if (u.isEmpty()) return Optional.empty();
            accounts.farmerAccount(u.get());               // creates it on first use
        }
        return accounts.payable(a.getBeneficiaryType(), a.getBeneficiaryId());
    }

    /** The platform share already sits in FarmConnect's own till: settle it with a ledger entry, no transfer. */
    private void retain(PaymentAllocation a, BeneficiaryAccount acct, long cents) {
        ledger.move(a.getPayment().getId(), a.getId(), "RETENTION",
                LedgerService.payable(a.getBeneficiaryType(), a.getBeneficiaryId()), LedgerService.PLATFORM_REVENUE, cents, "kept in FarmConnect till");
        a.setSettledCents(a.getAmountCents());
        a.setStatus(AllocationStatus.SETTLED);
        a.setSettledAt(Instant.now());
        Payout p = new Payout();
        p.setKind(PayoutKind.ALLOCATION);
        p.setAllocation(a);
        p.setPayment(a.getPayment());
        p.setBeneficiaryType(a.getBeneficiaryType());
        p.setBeneficiaryId(a.getBeneficiaryId());
        p.setAccountType(AccountType.RETAINED);
        p.setDestinationAccount("FARMCONNECT_TILL");
        p.setAmountCents(cents);
        p.setStatus(PayoutStatus.SUCCESS);
        p.setCompletedAt(Instant.now());
        payouts.save(p);
    }

    // ------------------------------------------------------------------------------------------ sending

    public int processQueue() {
        List<Long> ids = tx.execute(s -> payouts.findByStatusAndNextAttemptAtBeforeOrderByIdAsc(PayoutStatus.QUEUED, Instant.now())
                .stream().map(Payout::getId).toList());
        if (ids == null) return 0;
        ids.forEach(this::submit);
        return ids.size();
    }

    private record Job(AccountType type, String destination, String reference, long kes, String originId) {}

    public void submit(Long id) {
        Job job = tx.execute(s -> {
            Payout p = payouts.findById(id).orElse(null);
            if (p == null || p.getStatus() != PayoutStatus.QUEUED) return null;
            if (p.getAccountType() != AccountType.BANK && !mpesa.payoutsConfigured()) return null;   // not set up: wait, do not burn attempts
            if (payouts.claim(id, Instant.now()) != 1) return null;                                // somebody else has it
            p = payouts.findById(id).orElseThrow();
            if (p.getAccountType() == AccountType.BANK) {                                          // finance pays it by hand
                p.setProviderConversationId(MANUAL);
                p.setFailureReason("Awaiting manual bank transfer. Mark it paid once done.");
                payouts.save(p);
                return null;
            }
            String origin = UUID.randomUUID().toString();
            p.setOriginatorConversationId(origin);
            payouts.save(p);
            return new Job(p.getAccountType(), p.getDestinationAccount(), p.getDestinationReference(), p.getAmountCents() / 100, origin);
        });
        if (job == null) return;

        // The claim and origin id are committed. Only now is anything sent.
        MpesaService.SubmitResult r = switch (job.type()) {
            case MPESA_PHONE -> mpesa.b2c(job.originId(), job.destination(), job.kes(), "FarmConnect payout");
            case PAYBILL -> mpesa.b2b(job.originId(), false, job.destination(), job.reference(), job.kes(), "FarmConnect settlement");
            case TILL -> mpesa.b2b(job.originId(), true, job.destination(), job.reference(), job.kes(), "FarmConnect settlement");
            default -> new MpesaService.SubmitResult(MpesaService.SubmitOutcome.REJECTED, null, "Unsupported account type");
        };
        tx.executeWithoutResult(s -> afterSubmit(id, r));
    }

    private void afterSubmit(Long id, MpesaService.SubmitResult r) {
        Payout p = payouts.lockById(id).orElseThrow();
        if (p.getStatus() != PayoutStatus.PROCESSING) return;      // the result callback already beat us here
        switch (r.outcome()) {
            case ACCEPTED -> { p.setProviderConversationId(r.conversationId()); p.setFailureReason(null); }
            case REJECTED -> failOrRetry(p, r.detail());
            case UNKNOWN -> {
                p.setStatus(PayoutStatus.UNKNOWN);
                p.setFailureReason(trunc("Outcome unknown, check the M-Pesa statement before retrying: " + r.detail()));
                log.error("Payout #{} outcome UNKNOWN: {}", p.getId(), r.detail());
            }
        }
        payouts.save(p);
    }

    /** Back-off retry (4, 8, 16... minutes). After payments.max-attempts the payout FAILS and the allocation is put ON_HOLD. */
    private void failOrRetry(Payout p, String reason) {
        p.setFailureReason(trunc(reason));
        if (p.getAttemptCount() >= policy.getMaxAttempts()) {
            p.setStatus(PayoutStatus.FAILED);
            p.setCompletedAt(Instant.now());
            if (p.getAllocation() != null) {
                p.getAllocation().setStatus(AllocationStatus.ON_HOLD);
                p.getAllocation().setHoldReason(trunc("Payout failed " + p.getAttemptCount() + " times: " + reason));
            }
            log.error("Payout #{} FAILED permanently: {}", p.getId(), reason);
        } else {
            long minutes = Math.min(240, 2L << Math.min(p.getAttemptCount(), 7));
            p.setStatus(PayoutStatus.QUEUED);
            p.setNextAttemptAt(Instant.now().plus(Duration.ofMinutes(minutes)));
        }
    }

    // ------------------------------------------------------------------------------------------ results from Safaricom

    public void onResult(JsonNode body) {
        tx.executeWithoutResult(s -> {
            JsonNode r = body.path("Result");
            Payout p = find(r);
            if (p == null) { log.error("Payout result for an unknown conversation: {}", r); return; }
            if (p.getStatus() == PayoutStatus.SUCCESS) return;             // repeat delivery

            if (r.path("ResultCode").asInt(-1) == 0) {
                for (JsonNode it : r.path("ResultParameters").path("ResultParameter")) {
                    if ("TransactionAmount".equals(it.path("Key").asText())
                            && it.path("Value").decimalValue().movePointRight(2).longValue() != p.getAmountCents())
                        log.error("Payout #{}: Safaricom paid {} but we asked for {} cents", p.getId(), it.path("Value"), p.getAmountCents());
                }
                String txn = r.path("TransactionID").asText(null);
                if (txn != null && !txn.isBlank()) p.setProviderTransactionId(txn);
                if (p.getProviderConversationId() == null) p.setProviderConversationId(r.path("ConversationID").asText(null));
                finishSuccess(p);
            } else if (p.getStatus() == PayoutStatus.PROCESSING || p.getStatus() == PayoutStatus.UNKNOWN) {
                failOrRetry(p, "M-Pesa result " + r.path("ResultCode").asText() + ": " + r.path("ResultDesc").asText());
            }
            payouts.save(p);
        });
    }

    public void onTimeout(JsonNode body) {
        tx.executeWithoutResult(s -> {
            Payout p = find(body.path("Result"));
            if (p != null && p.getStatus() == PayoutStatus.PROCESSING) {
                p.setStatus(PayoutStatus.UNKNOWN);
                p.setFailureReason("Safaricom reported a timeout. Check the M-Pesa statement before retrying.");
                payouts.save(p);
            }
        });
    }

    private Payout find(JsonNode r) {
        String origin = r.path("OriginatorConversationID").asText("");
        if (!origin.isBlank()) { Optional<Payout> p = payouts.lockByOriginatorConversationId(origin); if (p.isPresent()) return p.get(); }
        String conv = r.path("ConversationID").asText("");
        if (!conv.isBlank()) { Optional<Payout> p = payouts.findByProviderConversationId(conv); if (p.isPresent()) return payouts.lockById(p.get().getId()).orElse(null); }
        return null;
    }

    /** Money is confirmed gone: settle the allocation (or reduce what we owe a buyer) and write the ledger. */
    private void finishSuccess(Payout p) {
        p.setStatus(PayoutStatus.SUCCESS);
        p.setCompletedAt(Instant.now());
        p.setFailureReason(null);
        String ref = "payout " + (p.getProviderTransactionId() == null ? p.getId() : p.getProviderTransactionId());
        if (p.getKind() == PayoutKind.REFUND) {
            Payment pay = p.getPayment();
            pay.setExcessCents(Math.max(0, pay.getExcessCents() - p.getAmountCents()));
            pay.setRefundedCents(pay.getRefundedCents() + p.getAmountCents());
            ledger.move(pay.getId(), null, "REFUND", LedgerService.payable(BeneficiaryType.BUYER, p.getBeneficiaryId()),
                    LedgerService.CLEARING, p.getAmountCents(), ref);
            return;
        }
        PaymentAllocation a = p.getAllocation();
        a.setSettledCents(a.getSettledCents() + p.getAmountCents());
        a.setHoldReason(null);
        if (a.getSettledCents() >= a.getAmountCents()) { a.setStatus(AllocationStatus.SETTLED); a.setSettledAt(Instant.now()); }
        ledger.move(p.getPayment().getId(), a.getId(), "SETTLEMENT",
                LedgerService.payable(a.getBeneficiaryType(), a.getBeneficiaryId()), LedgerService.CLEARING, p.getAmountCents(), ref);
        if (a.getBeneficiaryType() == BeneficiaryType.FARMER) {
            try {
                users.findById(Long.parseLong(a.getBeneficiaryId())).ifPresent(u -> notifier.notifyAndSms(u, "PAYMENT",
                        "KES " + p.getAmountCents() / 100 + " sent to your M-Pesa",
                        "Payment for order #" + a.getPayment().getOrder().getId() + " has been paid out.", a.getPayment().getOrder().getId()));
            } catch (Exception e) { log.warn("Could not notify farmer about payout #{}: {}", p.getId(), e.toString()); }
        }
    }

    /** PROCESSING with no answer from Safaricom for too long -> UNKNOWN (a person must check; never auto-retried). */
    public int sweepStale() {
        Integer n = tx.execute(s -> {
            int c = 0;
            for (Payout p : payouts.findByStatusAndLastAttemptAtBefore(PayoutStatus.PROCESSING,
                    Instant.now().minus(Duration.ofMinutes(policy.getProcessingTimeoutMinutes())))) {
                if (MANUAL.equals(p.getProviderConversationId())) continue;
                p.setStatus(PayoutStatus.UNKNOWN);
                p.setFailureReason("No result from Safaricom within " + policy.getProcessingTimeoutMinutes() + " minutes. Check the M-Pesa statement.");
                payouts.save(p);
                c++;
            }
            return c;
        });
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------------------------------ admin actions

    /** Re-queue a FAILED payout, or an UNKNOWN one once a person has confirmed on the statement that no money left. */
    public Payout requeue(Long id, boolean confirmedNotSent) {
        return tx.execute(s -> {
            Payout p = payouts.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));
            if (p.getStatus() == PayoutStatus.UNKNOWN && !confirmedNotSent)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Outcome is unknown. Check the M-Pesa statement; if no money left, retry with confirmNotSent=true. If it did, use mark-paid");
            if (p.getStatus() != PayoutStatus.FAILED && p.getStatus() != PayoutStatus.UNKNOWN)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only FAILED or UNKNOWN payouts can be retried");
            p.setStatus(PayoutStatus.QUEUED);
            p.setAttemptCount(0);
            p.setNextAttemptAt(Instant.now());
            p.setCompletedAt(null);
            p.setProviderConversationId(null);
            if (p.getAllocation() != null && p.getAllocation().getStatus() == AllocationStatus.ON_HOLD) {
                p.getAllocation().setStatus(AllocationStatus.SETTLING);
                p.getAllocation().setHoldReason(null);
            }
            return payouts.save(p);
        });
    }

    /** Money really left (seen in the M-Pesa statement or the bank): complete the payout and settle the allocation. */
    public Payout markPaid(Long id, String reference) {
        return tx.execute(s -> {
            Payout p = payouts.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));
            if (p.getStatus() == PayoutStatus.SUCCESS) return p;
            if (p.getStatus() == PayoutStatus.QUEUED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This payout has not been sent yet");
            if (reference == null || reference.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the M-Pesa or bank reference");
            p.setProviderTransactionId(reference.trim());
            finishSuccess(p);
            return payouts.save(p);
        });
    }

    public PaymentAllocation hold(Long allocationId, String reason) {
        return tx.execute(s -> {
            PaymentAllocation a = allocations.findById(allocationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allocation not found"));
            if (a.getStatus() != AllocationStatus.RECORDED && a.getStatus() != AllocationStatus.ELIGIBLE && a.getStatus() != AllocationStatus.SETTLING)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot hold an allocation that is " + a.getStatus());
            for (Payout p : payouts.findByAllocationId(allocationId)) {
                if (p.getStatus() == PayoutStatus.PROCESSING || p.getStatus() == PayoutStatus.UNKNOWN)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A payout is already in flight for this allocation");
                if (p.getStatus() == PayoutStatus.QUEUED) { p.setStatus(PayoutStatus.CANCELLED); payouts.save(p); }
            }
            a.setStatus(AllocationStatus.ON_HOLD);
            a.setHoldReason(trunc(reason == null || reason.isBlank() ? "Held by admin" : reason));
            return allocations.save(a);
        });
    }

    /** Back to RECORDED: the normal delivery + dispute-window rules apply again before it is paid. */
    public PaymentAllocation release(Long allocationId) {
        return tx.execute(s -> {
            PaymentAllocation a = allocations.findById(allocationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allocation not found"));
            if (a.getStatus() != AllocationStatus.ON_HOLD) throw new ResponseStatusException(HttpStatus.CONFLICT, "Allocation is not on hold");
            a.setStatus(AllocationStatus.RECORDED);
            a.setHoldReason(null);
            return allocations.save(a);
        });
    }

    private static String trunc(String s) { return s == null ? null : s.length() > 490 ? s.substring(0, 490) : s; }
}
