package com.farmconnect.service;

import com.farmconnect.dto.SupplyDtos.SupplyPaymentView;
import com.farmconnect.model.*;
import com.farmconnect.payment.repository.PaymentRepository;
import com.farmconnect.payment.service.LedgerService;
import com.farmconnect.payment.service.PaymentPolicy;
import com.farmconnect.repository.SupplyOrderRepository;
import com.farmconnect.repository.SupplyPaymentRepository;
import com.farmconnect.repository.SupplyPayoutRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * M-Pesa collection for supply orders. A farmer can pay only AFTER the order is DELIVERED, so no money is ever held
 * for goods that were not handed over. The money rules mirror the produce payments: the payment row is locked, the
 * receipt must be new, the amount must cover the order, and the collection, the split and the ledger lines are written
 * in ONE database transaction. Anything doubtful goes to NEEDS_REVIEW for a person. Switched off unless
 * supply.payments.enabled=true.
 */
@Service
@RequiredArgsConstructor
public class SupplyPaymentService {
    private static final Logger log = LoggerFactory.getLogger(SupplyPaymentService.class);

    private final SupplyPaymentRepository payments;
    private final SupplyOrderRepository orders;
    private final SupplyPayoutRepository payouts;
    private final PaymentRepository producePayments;
    private final SupplyLedger ledger;
    private final MpesaService mpesa;
    private final PaymentPolicy policy;
    private final NotificationService notifier;
    private final TransactionTemplate tx;

    @Value("${supply.payments.enabled:false}") private boolean enabled;

    public boolean enabled() { return enabled; }

    /** What the farmer must pay: the order total rounded UP to whole shillings (M-Pesa has no cents), in cents. */
    public static long dueCents(SupplyOrder o) {
        return Math.max(1, o.getTotal().setScale(0, RoundingMode.CEILING).longValueExact()) * 100;
    }

    // ------------------------------------------------------------------------------------------ start

    private record Prep(long dueCents, String ref) {}

    public SupplyPaymentView startStk(User buyer, Long orderId, String phoneRaw) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "M-Pesa payment for farm inputs is not switched on yet. Pay the company directly.");
        String phone = MpesaService.normalizePhone(phoneRaw);
        Prep prep = tx.execute(s -> {
            SupplyOrder o = orders.lockById(orderId).filter(x -> x.getBuyer().getId().equals(buyer.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            if (o.getStatus() != SupplyOrderStatus.DELIVERED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You can pay once the order is delivered");
            if ("PAID".equals(o.getPaymentStatus()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already paid");
            if (payments.existsByOrderIdAndStatusAndCreatedAtAfter(o.getId(), SupplyPaymentStatus.PENDING, Instant.now().minusSeconds(180)))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A payment prompt was just sent to your phone. Check it.");
            return new Prep(dueCents(o), "SO" + o.getId());
        });
        // the prompt is sent outside the database transaction
        MpesaService.StkResult r = mpesa.stkPushFull(phone, BigDecimal.valueOf(prep.dueCents() / 100), prep.ref(), "FarmConnect");
        return tx.execute(s -> {
            SupplyOrder o = orders.lockById(orderId).orElseThrow();
            SupplyPayment p = new SupplyPayment();
            p.setOrder(o);
            p.setMerchantRequestId(r.merchantRequestId());
            p.setCheckoutRequestId(r.checkoutRequestId());
            p.setExpectedCents(prep.dueCents());
            p.setPhoneNumber(phone);
            p = payments.save(p);
            if (!"PAID".equals(o.getPaymentStatus())) {
                o.setPaymentStatus("PENDING");
                o.setUpdatedAt(Instant.now());
                orders.save(o);
            }
            return view(p);
        });
    }

    // ------------------------------------------------------------------------------------------ STK callback

    /** Returns false when the CheckoutRequestID is not one of ours, so the produce payments can try it. */
    @Transactional
    public boolean onStkCallback(JsonNode body) {
        JsonNode cb = body.path("Body").path("stkCallback");
        String checkoutId = cb.path("CheckoutRequestID").asText("");
        if (checkoutId.isBlank()) return false;
        SupplyPayment p = payments.lockByCheckoutRequestId(checkoutId).orElse(null);
        if (p == null) return false;

        boolean open = p.getStatus() == SupplyPaymentStatus.PENDING
                || (p.getStatus() == SupplyPaymentStatus.NEEDS_REVIEW && p.getMpesaReceiptNumber() == null);
        if (!open) return true;                                    // Safaricom retries callbacks: stay idempotent

        if (cb.path("ResultCode").asInt(-1) != 0) {
            if (p.getStatus() == SupplyPaymentStatus.PENDING) {
                p.setStatus(SupplyPaymentStatus.FAILED);
                p.setCallbackReceivedAt(Instant.now());
                addNote(p, cb.path("ResultDesc").asText("Payment not completed"));
                payments.save(p);
                unmarkPending(p.getOrder());
            }
            return true;
        }
        String receipt = null, phone = null;
        BigDecimal amount = null;
        for (JsonNode it : cb.path("CallbackMetadata").path("Item")) {
            String n = it.path("Name").asText();
            if (n.equals("MpesaReceiptNumber")) receipt = it.path("Value").asText(null);
            if (n.equals("Amount")) amount = it.path("Value").decimalValue();
            if (n.equals("PhoneNumber")) phone = it.path("Value").asText(null);
        }
        complete(p, toCents(amount), receipt, phone);
        return true;
    }

    // ------------------------------------------------------------------------------------------ the one place money is accepted

    /** Runs inside the caller's transaction with the payment row locked. If anything throws, nothing is saved. */
    private void complete(SupplyPayment p, long paidCents, String receiptRaw, String callbackPhone) {
        p.setCallbackReceivedAt(Instant.now());
        SupplyOrder o = orders.lockById(p.getOrder().getId()).orElseThrow();

        if (receiptRaw == null || receiptRaw.isBlank()) {
            p.setStatus(SupplyPaymentStatus.NEEDS_REVIEW);
            p.setAmountCents(paidCents);
            addNote(p, "No M-Pesa receipt number in the notification");
            payments.save(p);
            log.error("Supply payment #{} needs review: no receipt", p.getId());
            return;
        }
        String receipt = receiptRaw.trim().toUpperCase();
        boolean duplicate = payments.findByMpesaReceiptNumber(receipt).filter(x -> !x.getId().equals(p.getId())).isPresent()
                || producePayments.findByMpesaReceiptNumber(receipt).isPresent();
        if (duplicate) {
            p.setStatus(SupplyPaymentStatus.NEEDS_REVIEW);
            p.setAmountCents(paidCents);
            addNote(p, "Receipt " + receipt + " was already used by another payment");
            payments.save(p);
            log.error("Supply payment #{} needs review: duplicate receipt {}", p.getId(), receipt);
            return;
        }

        // Money has arrived. Record it first.
        p.setMpesaReceiptNumber(receipt);
        p.setAmountCents(paidCents);
        ledger.move(p.getId(), "COLLECTION", LedgerService.CLEARING, LedgerService.COLLECTED, paidCents, "receipt " + receipt);

        if (paidCents < p.getExpectedCents()) {
            p.setStatus(SupplyPaymentStatus.NEEDS_REVIEW);
            owe(p, o, paidCents, "underpayment");
            addNote(p, "Paid KES " + paidCents / 100.0 + " but the order total is KES " + p.getExpectedCents() / 100.0
                    + ". Nothing allocated. Refund the farmer or ask them to pay again.");
            payments.save(p);
            unmarkPending(o);
            return;
        }
        boolean orderOpen = !"PAID".equals(o.getPaymentStatus()) && o.getStatus() == SupplyOrderStatus.DELIVERED;
        if (!orderOpen) {
            p.setStatus(SupplyPaymentStatus.SUCCESS);
            owe(p, o, paidCents, "duplicate or late payment");
            addNote(p, "Order #" + o.getId() + " was already paid. Full amount is owed back to the farmer.");
            payments.save(p);
            log.error("Supply payment #{} arrived for order {} which is already {}. Refund due.", p.getId(), o.getId(), o.getPaymentStatus());
            return;
        }

        long sale = p.getExpectedCents();                           // never allocate more than the order is worth
        p.setStatus(SupplyPaymentStatus.SUCCESS);
        long saleKes = sale / 100;
        long platformKes = shillings(saleKes, o.getPlatformBps());
        long countyKes = shillings(saleKes, o.getCountyBps());
        long companyKes = saleKes - platformKes - countyKes;
        if (companyKes < 0) throw new IllegalStateException("Shares exceed the sale for order " + o.getId());
        ledger.move(p.getId(), "ALLOCATION", LedgerService.COLLECTED, LedgerService.PLATFORM_REVENUE, platformKes * 100, "platform share");
        ledger.move(p.getId(), "ALLOCATION", LedgerService.COLLECTED, SupplyLedger.PAYABLE_COUNTY, countyKes * 100, "county share");
        ledger.move(p.getId(), "ALLOCATION", LedgerService.COLLECTED, SupplyLedger.payableCompany(o.getCompany().getId()), companyKes * 100, "company share");
        if (paidCents > sale) owe(p, o, paidCents - sale, "overpayment");
        payments.save(p);

        o.setPlatformFee(BigDecimal.valueOf(platformKes));
        o.setCountyFee(BigDecimal.valueOf(countyKes));
        o.setCompanyNet(BigDecimal.valueOf(companyKes));
        o.setPaymentStatus("PAID");
        o.setPaymentMethod("MPESA");
        o.setMpesaReceipt(receipt);
        o.setPaidAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        orders.save(o);

        if (companyKes > 0) {
            SupplyPayout po = new SupplyPayout();
            po.setOrder(o);
            po.setPayment(p);
            po.setCompany(o.getCompany());
            po.setAmountCents(companyKes * 100);
            payouts.save(po);
        }
        try {
            notifier.notifyAndSms(o.getBuyer(), "SUPPLY", "Payment received for order #" + o.getId(), "M-Pesa receipt " + receipt + ". Thank you!", o.getId());
            notifier.notify(o.getCompany().getOwner(), "SUPPLY", "Order #" + o.getId() + " is paid",
                    "The farmer paid by M-Pesa. Your share, KSh " + companyKes + ", is being sent to your payout account.", o.getId());
        } catch (Exception e) {
            log.warn("Could not send supply payment notifications for order {}: {}", o.getId(), e.toString());   // never undo money because a push failed
        }
    }

    /** whole shillings, rounded half up: saleKes x bps / 10000 */
    static long shillings(long saleKes, int bps) {
        return SupplyMath.fee(BigDecimal.valueOf(saleKes), bps).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private void owe(SupplyPayment p, SupplyOrder o, long cents, String why) {
        if (cents <= 0) return;
        ledger.move(p.getId(), "EXCESS", LedgerService.COLLECTED, SupplyLedger.payableBuyer(o.getBuyer().getId()), cents, why);
        p.setExcessCents(p.getExcessCents() + cents);
    }

    private void unmarkPending(SupplyOrder o) {
        if ("PENDING".equals(o.getPaymentStatus())) {
            o.setPaymentStatus("UNPAID");
            o.setUpdatedAt(Instant.now());
            orders.save(o);
        }
    }

    // ------------------------------------------------------------------------------------------ lost callbacks

    /** Asks Safaricom about STK pushes that got no callback. Network calls stay outside transactions. */
    public void sweepPending() {
        if (!enabled) return;
        Instant now = Instant.now();
        for (SupplyPayment p : payments.findByStatusAndCreatedAtBefore(SupplyPaymentStatus.PENDING, now.minus(Duration.ofMinutes(policy.getPendingCheckMinutes())))) {
            if (p.getCheckoutRequestId() == null || p.getCreatedAt().isBefore(now.minus(Duration.ofHours(24)))) continue;
            MpesaService.StkQueryResult q = mpesa.stkQuery(p.getCheckoutRequestId());
            if (!q.answered()) continue;
            Long id = p.getId();
            tx.executeWithoutResult(s -> payments.lockById(id).ifPresent(locked -> {
                if (locked.getStatus() != SupplyPaymentStatus.PENDING) return;
                if (q.resultCode() == 0) {
                    locked.setStatus(SupplyPaymentStatus.NEEDS_REVIEW);
                    addNote(locked, "Safaricom says this payment succeeded but no callback arrived. Find the receipt in the M-Pesa statement and confirm it.");
                } else {
                    locked.setStatus(SupplyPaymentStatus.FAILED);
                    addNote(locked, q.description());
                    unmarkPending(locked.getOrder());
                }
                payments.save(locked);
            }));
        }
    }

    // ------------------------------------------------------------------------------------------ admin

    @Transactional
    public SupplyPaymentView adminConfirm(Long paymentId, String receipt, long amountKes, Long adminId) {
        SupplyPayment p = payments.lockById(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (p.getStatus() != SupplyPaymentStatus.PENDING && p.getStatus() != SupplyPaymentStatus.NEEDS_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment is already " + p.getStatus());
        if (p.getMpesaReceiptNumber() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment already has a receipt");
        addNote(p, "Confirmed by admin #" + adminId);
        complete(p, Math.multiplyExact(amountKes, 100L), receipt.trim(), null);
        return view(p);
    }

    @Transactional(readOnly = true)
    public List<SupplyPaymentView> list(SupplyPaymentStatus status) {
        return (status == null ? payments.findTop100ByOrderByIdDesc() : payments.findByStatusOrderByIdDesc(status)).stream().map(SupplyPaymentService::view).toList();
    }

    // ------------------------------------------------------------------------------------------ helpers

    static long toCents(BigDecimal kes) {
        return kes == null ? 0 : kes.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    static void addNote(SupplyPayment p, String text) {
        String n = p.getNote() == null || p.getNote().isBlank() ? text : p.getNote() + " | " + text;
        p.setNote(n.length() > 500 ? n.substring(n.length() - 500) : n);
    }

    static SupplyPaymentView view(SupplyPayment p) {
        return new SupplyPaymentView(p.getId(), p.getOrder().getId(), p.getOrder().getBuyer().getName(), p.getStatus().name(),
                p.getExpectedCents() / 100, p.getAmountCents() / 100, p.getExcessCents() / 100, p.getMpesaReceiptNumber(),
                p.getPhoneNumber(), p.getNote(), p.getCreatedAt());
    }
}
