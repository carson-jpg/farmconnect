package com.farmconnect.service;

import com.farmconnect.dto.SupplyDtos.PayoutView;
import com.farmconnect.model.*;
import com.farmconnect.payment.service.LedgerService;
import com.farmconnect.payment.service.PaymentPolicy;
import com.farmconnect.repository.SupplyPayoutRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sends each company its share after a farmer's payment is collected. Same safety rules as the produce payouts:
 * the claim is atomic, the originator id is saved BEFORE anything is sent, an unclear outcome becomes UNKNOWN and is
 * never retried automatically, and the ledger is written in the same transaction as the SUCCESS status.
 * Only the company's payout account that the super admin approved is ever paid.
 */
@Service
@RequiredArgsConstructor
public class SupplyPayoutService {
    private static final Logger log = LoggerFactory.getLogger(SupplyPayoutService.class);
    public static final String PREFIX = "SUP-";

    private final SupplyPayoutRepository payouts;
    private final SupplyLedger ledger;
    private final MpesaService mpesa;
    private final PaymentPolicy policy;
    private final NotificationService notifier;
    private final TransactionTemplate tx;

    // ------------------------------------------------------------------------------------------ sending

    public int processQueue() {
        List<Long> ids = tx.execute(s -> payouts.findByStatusAndNextAttemptAtBeforeOrderByIdAsc(SupplyPayoutStatus.QUEUED, Instant.now())
                .stream().map(SupplyPayout::getId).toList());
        if (ids == null) return 0;
        ids.forEach(this::submit);
        return ids.size();
    }

    private record Job(PayoutAccountType type, String destination, String reference, long kes, String origin) {}

    public void submit(Long id) {
        Job job = tx.execute(s -> {
            SupplyPayout p = payouts.findById(id).orElse(null);
            if (p == null || p.getStatus() != SupplyPayoutStatus.QUEUED) return null;
            if (!mpesa.payoutsConfigured()) return null;                       // not set up: wait, do not burn attempts
            Company c = p.getCompany();
            if (!c.isPayoutApproved()) {
                p.setFailureReason("Waiting for the super admin to approve the company's payout account");
                p.setNextAttemptAt(Instant.now().plus(Duration.ofMinutes(30)));
                payouts.save(p);
                return null;
            }
            if (p.getAmountCents() / 100 < policy.getMinPayoutKes()) {
                p.setFailureReason("Below the M-Pesa minimum payout of KES " + policy.getMinPayoutKes() + ". An admin decides.");
                p.setNextAttemptAt(Instant.now().plus(Duration.ofDays(1)));
                payouts.save(p);
                return null;
            }
            if (payouts.claim(id, Instant.now()) != 1) return null;           // somebody else has it
            p = payouts.findById(id).orElseThrow();
            String origin = PREFIX + UUID.randomUUID();
            p.setOriginatorConversationId(origin);
            p.setAccountType(c.getPayoutType());
            p.setDestinationAccount(c.getPayoutAccount());
            p.setDestinationReference(c.getPayoutReference());
            p.setFailureReason(null);
            payouts.save(p);
            return new Job(c.getPayoutType(), c.getPayoutAccount(), c.getPayoutReference(), p.getAmountCents() / 100, origin);
        });
        if (job == null) return;

        // The claim and the origin id are committed. Only now is anything sent.
        MpesaService.SubmitResult r = switch (job.type()) {
            case MPESA_PHONE -> mpesa.b2c(job.origin(), job.destination(), job.kes(), "FarmConnect supply payout");
            case PAYBILL -> mpesa.b2b(job.origin(), false, job.destination(), job.reference(), job.kes(), "FarmConnect supply settlement");
            case TILL -> mpesa.b2b(job.origin(), true, job.destination(), job.reference(), job.kes(), "FarmConnect supply settlement");
        };
        tx.executeWithoutResult(s -> afterSubmit(id, r));
    }

    private void afterSubmit(Long id, MpesaService.SubmitResult r) {
        SupplyPayout p = payouts.lockById(id).orElseThrow();
        if (p.getStatus() != SupplyPayoutStatus.PROCESSING) return;           // the result callback already beat us here
        switch (r.outcome()) {
            case ACCEPTED -> { p.setProviderConversationId(r.conversationId()); p.setFailureReason(null); }
            case REJECTED -> failOrRetry(p, r.detail());
            case UNKNOWN -> {
                p.setStatus(SupplyPayoutStatus.UNKNOWN);
                p.setFailureReason(trunc("Outcome unknown, check the M-Pesa statement before retrying: " + r.detail()));
                log.error("Supply payout #{} outcome UNKNOWN: {}", p.getId(), r.detail());
            }
        }
        payouts.save(p);
    }

    /** Back-off retry (4, 8, 16... minutes). After max attempts the payout FAILS and waits for an admin. */
    private void failOrRetry(SupplyPayout p, String reason) {
        p.setFailureReason(trunc(reason));
        if (p.getAttemptCount() >= policy.getMaxAttempts()) {
            p.setStatus(SupplyPayoutStatus.FAILED);
            p.setCompletedAt(Instant.now());
            log.error("Supply payout #{} FAILED permanently: {}", p.getId(), reason);
        } else {
            long minutes = Math.min(240, 2L << Math.min(p.getAttemptCount(), 7));
            p.setStatus(SupplyPayoutStatus.QUEUED);
            p.setNextAttemptAt(Instant.now().plus(Duration.ofMinutes(minutes)));
        }
    }

    // ------------------------------------------------------------------------------------------ results from Safaricom

    /** Returns false when the result is not about a supply payout, so the produce payouts can try it. */
    public boolean onResult(JsonNode body) {
        Boolean handled = tx.execute(s -> {
            JsonNode r = body.path("Result");
            SupplyPayout p = find(r);
            if (p == null) return false;
            if (p.getStatus() == SupplyPayoutStatus.SUCCESS) return true;     // repeat delivery
            if (r.path("ResultCode").asInt(-1) == 0) {
                for (JsonNode it : r.path("ResultParameters").path("ResultParameter")) {
                    if ("TransactionAmount".equals(it.path("Key").asText())
                            && it.path("Value").decimalValue().movePointRight(2).longValue() != p.getAmountCents())
                        log.error("Supply payout #{}: Safaricom paid {} but we asked for {} cents", p.getId(), it.path("Value"), p.getAmountCents());
                }
                String txn = r.path("TransactionID").asText(null);
                if (txn != null && !txn.isBlank()) p.setProviderTransactionId(txn);
                if (p.getProviderConversationId() == null) p.setProviderConversationId(r.path("ConversationID").asText(null));
                finishSuccess(p);
            } else if (p.getStatus() == SupplyPayoutStatus.PROCESSING || p.getStatus() == SupplyPayoutStatus.UNKNOWN) {
                failOrRetry(p, "M-Pesa result " + r.path("ResultCode").asText() + ": " + r.path("ResultDesc").asText());
            }
            payouts.save(p);
            return true;
        });
        return Boolean.TRUE.equals(handled);
    }

    public boolean onTimeout(JsonNode body) {
        Boolean handled = tx.execute(s -> {
            SupplyPayout p = find(body.path("Result"));
            if (p == null) return false;
            if (p.getStatus() == SupplyPayoutStatus.PROCESSING) {
                p.setStatus(SupplyPayoutStatus.UNKNOWN);
                p.setFailureReason("Safaricom reported a timeout. Check the M-Pesa statement before retrying.");
                payouts.save(p);
            }
            return true;
        });
        return Boolean.TRUE.equals(handled);
    }

    private SupplyPayout find(JsonNode r) {
        String origin = r.path("OriginatorConversationID").asText("");
        if (origin.startsWith(PREFIX)) return payouts.lockByOriginatorConversationId(origin).orElse(null);
        String conv = r.path("ConversationID").asText("");
        if (!conv.isBlank()) return payouts.findByProviderConversationId(conv).flatMap(x -> payouts.lockById(x.getId())).orElse(null);
        return null;
    }

    /** Money is confirmed gone: write the ledger and tell the company. */
    private void finishSuccess(SupplyPayout p) {
        p.setStatus(SupplyPayoutStatus.SUCCESS);
        p.setCompletedAt(Instant.now());
        p.setFailureReason(null);
        String ref = "payout " + (p.getProviderTransactionId() == null ? p.getId() : p.getProviderTransactionId());
        ledger.move(p.getPayment().getId(), "SETTLEMENT", SupplyLedger.payableCompany(p.getCompany().getId()), LedgerService.CLEARING, p.getAmountCents(), ref);
        try {
            notifier.notifyAndSms(p.getCompany().getOwner(), "SUPPLY", "KES " + p.getAmountCents() / 100 + " sent to your payout account",
                    "Payment for order #" + p.getOrder().getId() + " has been paid out.", p.getOrder().getId());
        } catch (Exception e) {
            log.warn("Could not notify company about supply payout #{}: {}", p.getId(), e.toString());
        }
    }

    /** PROCESSING with no answer from Safaricom for too long -> UNKNOWN (a person must check; never auto-retried). */
    public int sweepStale() {
        Integer n = tx.execute(s -> {
            int c = 0;
            for (SupplyPayout p : payouts.findByStatusAndLastAttemptAtBefore(SupplyPayoutStatus.PROCESSING,
                    Instant.now().minus(Duration.ofMinutes(policy.getProcessingTimeoutMinutes())))) {
                p.setStatus(SupplyPayoutStatus.UNKNOWN);
                p.setFailureReason("No answer from Safaricom. Check the M-Pesa statement before retrying.");
                payouts.save(p);
                c++;
            }
            return c;
        });
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------------------------------ admin and views

    /** UNKNOWN needs confirmedNotSent=true: the admin looked at the M-Pesa statement and the money did NOT go. */
    @Transactional
    public PayoutView requeue(Long id, boolean confirmedNotSent) {
        SupplyPayout p = payouts.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));
        if (p.getStatus() != SupplyPayoutStatus.FAILED && p.getStatus() != SupplyPayoutStatus.UNKNOWN)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a failed or unknown payout can be requeued");
        if (p.getStatus() == SupplyPayoutStatus.UNKNOWN && !confirmedNotSent)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check the M-Pesa statement. Requeue only if the money was NOT sent.");
        p.setStatus(SupplyPayoutStatus.QUEUED);
        p.setAttemptCount(0);
        p.setNextAttemptAt(Instant.now());
        p.setFailureReason(null);
        p.setOriginatorConversationId(null);
        return view(payouts.save(p));
    }

    /** Finance paid it by hand (or confirmed it went through): record it. */
    @Transactional
    public PayoutView markPaid(Long id, String reference) {
        SupplyPayout p = payouts.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));
        if (p.getStatus() == SupplyPayoutStatus.SUCCESS || p.getStatus() == SupplyPayoutStatus.PROCESSING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payout is already " + p.getStatus());
        p.setProviderTransactionId(reference == null || reference.isBlank() ? "MANUAL" : reference.trim());
        finishSuccess(p);
        return view(payouts.save(p));
    }

    @Transactional(readOnly = true)
    public List<PayoutView> list(SupplyPayoutStatus status) {
        return (status == null ? payouts.findTop100ByOrderByIdDesc() : payouts.findByStatusOrderByIdDesc(status)).stream().map(SupplyPayoutService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<PayoutView> forCompany(Long companyId) {
        return payouts.findByCompanyIdOrderByIdDesc(companyId).stream().map(SupplyPayoutService::view).toList();
    }

    static PayoutView view(SupplyPayout p) {
        return new PayoutView(p.getId(), p.getOrder().getId(), p.getCompany().getName(), p.getAmountCents() / 100, p.getStatus().name(),
                p.getFailureReason(), p.getProviderTransactionId(), p.getCreatedAt(), p.getCompletedAt());
    }

    private static String trunc(String s) { return s == null ? null : s.length() > 490 ? s.substring(0, 490) : s; }
}
