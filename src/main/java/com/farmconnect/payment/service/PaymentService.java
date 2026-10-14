package com.farmconnect.payment.service;

import com.farmconnect.model.CustomerOrder;
import com.farmconnect.model.OrderItem;
import com.farmconnect.model.OrderStatus;
import com.farmconnect.model.User;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.PaymentRepository;
import com.farmconnect.repository.OrderRepository;
import com.farmconnect.service.MpesaService;
import com.farmconnect.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collection side of the system: starts payments, turns callbacks into verified payments, and - in the SAME database
 * transaction - records the three allocations and their ledger lines. Repeated callbacks change nothing.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final MpesaService mpesa;
    private final PaymentVerificationService verifier;
    private final CommissionService commission;
    private final LedgerService ledger;
    private final PaymentPolicy policy;
    private final NotificationService notifier;
    private final TransactionTemplate tx;

    /** What the buyer must pay: the order total rounded UP to whole shillings (M-Pesa has no cents), in cents. */
    public static long dueCents(CustomerOrder o) {
        long kes = Math.max(1, o.getTotal().setScale(0, RoundingMode.CEILING).longValueExact());
        return kes * 100;
    }

    // ------------------------------------------------------------------------------------------ start

    /** Sends the STK prompt and records a PENDING payment. The amount always comes from the order on the server. */
    @Transactional
    public Payment startStk(CustomerOrder o, String phone254) {
        MpesaService.StkResult r = mpesa.stkPushFull(phone254, o.getTotal(), "FC" + o.getId(), "FarmConnect");
        Payment p = new Payment();
        p.setOrder(o);
        p.setProvider("MPESA_STK");
        p.setMerchantRequestId(r.merchantRequestId());
        p.setCheckoutRequestId(r.checkoutRequestId());
        p.setExpectedCents(dueCents(o));
        p.setPhoneNumber(phone254);
        p.setStatus(PaymentStatus.PENDING);
        p = payments.save(p);
        o.setMpesaCheckoutRequestId(r.checkoutRequestId());
        o.setPaymentStatus("PENDING");
        o.setUpdatedAt(Instant.now());
        orders.save(o);
        return p;
    }

    // ------------------------------------------------------------------------------------------ STK callback

    @Transactional
    public void onStkCallback(JsonNode body) {
        JsonNode cb = body.path("Body").path("stkCallback");
        String checkoutId = cb.path("CheckoutRequestID").asText("");
        if (checkoutId.isBlank()) { log.warn("STK callback without CheckoutRequestID"); return; }

        Payment p = payments.lockByCheckoutRequestId(checkoutId).orElseGet(() -> legacyPayment(checkoutId));
        if (p == null) { log.warn("M-Pesa callback for unknown CheckoutRequestID {}", checkoutId); return; }

        boolean open = p.getStatus() == PaymentStatus.PENDING
                || (p.getStatus() == PaymentStatus.NEEDS_REVIEW && p.getMpesaReceiptNumber() == null);
        if (!open) return;                                    // Safaricom retries callbacks: stay idempotent

        if (cb.path("ResultCode").asInt(-1) != 0) {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.FAILED);
                p.setCallbackReceivedAt(Instant.now());
                addNote(p, cb.path("ResultDesc").asText("Payment not completed"));
                payments.save(p);
                markOrderFailed(p.getOrder());
            }
            return;
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
    }

    /** Orders that started an STK push before the payments table existed have no Payment row; make one on the fly. */
    private Payment legacyPayment(String checkoutId) {
        CustomerOrder o = orders.findByMpesaCheckoutRequestId(checkoutId).orElse(null);
        if (o == null) return null;
        Payment p = new Payment();
        p.setOrder(o);
        p.setProvider("MPESA_STK");
        p.setCheckoutRequestId(checkoutId);
        p.setExpectedCents(dueCents(o));
        p.setStatus(PaymentStatus.PENDING);
        return payments.save(p);
    }

    // ------------------------------------------------------------------------------------------ C2B (customer pays the till/paybill directly)

    /** Safaricom asks: should this payment be accepted? Only for an open order reference like FC123. */
    @Transactional(readOnly = true)
    public boolean c2bAcceptable(JsonNode b) {
        CustomerOrder o = orderFromRef(b.path("BillRefNumber").asText(""));
        return o != null && !"PAID".equals(o.getPaymentStatus()) && o.getStatus() != OrderStatus.CANCELLED;
    }

    @Transactional
    public void onC2bConfirmation(JsonNode b) {
        String receipt = b.path("TransID").asText("").trim();
        if (receipt.isEmpty()) { log.error("C2B confirmation without TransID: {}", b); return; }
        if (payments.findByMpesaReceiptNumber(receipt).isPresent()) return;       // duplicate delivery
        CustomerOrder o = orderFromRef(b.path("BillRefNumber").asText(""));
        if (o == null) {
            log.error("C2B payment {} KES {} has no matching order (ref '{}'). Money received but unmatched: refund or match it manually.",
                    receipt, b.path("TransAmount").asText(), b.path("BillRefNumber").asText());
            return;
        }
        Payment p = new Payment();
        p.setOrder(o);
        p.setProvider("MPESA_C2B");
        p.setExpectedCents(dueCents(o));
        p.setStatus(PaymentStatus.PENDING);
        p = payments.saveAndFlush(p);
        complete(p, toCents(new BigDecimal(b.path("TransAmount").asText("0"))), receipt, null);
    }

    private CustomerOrder orderFromRef(String ref) {
        String digits = ref == null ? "" : ref.trim().toUpperCase().replaceFirst("^FC", "");
        if (!digits.matches("\\d{1,18}")) return null;
        return orders.findById(Long.parseLong(digits)).orElse(null);
    }

    // ------------------------------------------------------------------------------------------ the one place money is accepted

    /**
     * Runs inside the caller's transaction with the payment row locked. Verifies, then in this one transaction:
     * marks the payment SUCCESS, writes the COLLECTION ledger lines and creates all three allocations.
     * If anything throws, nothing at all is saved.
     */
    private void complete(Payment p, long paidCents, String receipt, String callbackPhone) {
        p.setCallbackReceivedAt(Instant.now());
        PaymentVerificationService.Verdict v = verifier.verify(p, paidCents, receipt, callbackPhone);
        CustomerOrder o = p.getOrder();

        if (v.kind() == PaymentVerificationService.Kind.NO_RECEIPT || v.kind() == PaymentVerificationService.Kind.DUPLICATE_RECEIPT) {
            p.setStatus(PaymentStatus.NEEDS_REVIEW);
            p.setAmountCents(paidCents);
            addNote(p, v.reason());
            payments.save(p);
            log.error("Payment #{} needs review: {}", p.getId(), v.reason());
            return;
        }

        // Money has arrived. Record it first.
        p.setMpesaReceiptNumber(receipt.trim());
        p.setAmountCents(paidCents);
        ledger.move(p.getId(), null, "COLLECTION", LedgerService.CLEARING, LedgerService.COLLECTED, paidCents, "receipt " + receipt);

        if (v.kind() == PaymentVerificationService.Kind.UNDERPAID) {
            p.setStatus(PaymentStatus.NEEDS_REVIEW);
            owe(p, paidCents, "underpayment");
            addNote(p, v.reason() + ". Nothing allocated. Refund the buyer or ask them to pay again.");
            payments.save(p);
            markOrderFailed(o);
            log.error("Order {} underpaid: {}", o.getId(), v.reason());
            return;
        }

        boolean orderOpen = !"PAID".equals(o.getPaymentStatus()) && o.getStatus() != OrderStatus.CANCELLED;
        if (!orderOpen) {                                      // a second payment for an order already paid, or a late one for a cancelled order
            p.setStatus(PaymentStatus.SUCCESS);
            owe(p, paidCents, "duplicate or late payment");
            addNote(p, "Order #" + o.getId() + " was already paid or cancelled. Full amount is owed back to the buyer.");
            payments.save(p);
            log.error("Payment #{} arrived for order {} which is {} / {}. Refund due.", p.getId(), o.getId(), o.getStatus(), o.getPaymentStatus());
            return;
        }

        long sale = p.getExpectedCents();                      // never allocate more than the order is worth
        p.setStatus(PaymentStatus.SUCCESS);
        payments.save(p);
        commission.allocate(p, o, sale);
        if (paidCents > sale) owe(p, paidCents - sale, "overpayment");
        payments.save(p);

        o.setPaymentStatus("PAID");
        o.setMpesaReceipt(receipt.trim());
        if (p.getCheckoutRequestId() != null) o.setMpesaCheckoutRequestId(p.getCheckoutRequestId());
        o.setUpdatedAt(Instant.now());
        orders.save(o);
        notifyPaid(o, receipt.trim());
    }

    private void owe(Payment p, long cents, String why) {
        if (cents <= 0) return;
        String buyer = String.valueOf(p.getOrder().getBuyer().getId());
        ledger.move(p.getId(), null, "EXCESS", LedgerService.COLLECTED, LedgerService.payable(BeneficiaryType.BUYER, buyer), cents, why);
        p.setExcessCents(p.getExcessCents() + cents);
    }

    private void markOrderFailed(CustomerOrder o) {
        if (!"PAID".equals(o.getPaymentStatus())) {
            o.setPaymentStatus("FAILED");
            o.setUpdatedAt(Instant.now());
            orders.save(o);
        }
    }

    private void notifyPaid(CustomerOrder o, String receipt) {
        try {
            notifier.notifyAndSms(o.getBuyer(), "ORDER", "Payment received for order #" + o.getId(),
                    "M-Pesa receipt " + receipt + ". Thank you!", o.getId());
            Map<Long, User> owners = new LinkedHashMap<>();
            for (OrderItem i : o.getItems()) {
                User f = i.getProduct().getFarm().getOwner();
                owners.putIfAbsent(f.getId(), f);
            }
            owners.values().forEach(f -> notifier.notifyAndSms(f, "ORDER", "Order #" + o.getId() + " is paid",
                    "The buyer paid by M-Pesa. You can confirm the order now.", o.getId()));
        } catch (Exception e) {
            log.warn("Could not send payment notifications for order {}: {}", o.getId(), e.toString());   // never undo money because a push failed
        }
    }

    // ------------------------------------------------------------------------------------------ lost callbacks

    /** Asks Safaricom about STK pushes that got no callback. Runs from the worker; network calls stay outside transactions. */
    public void sweepPending() {
        Instant now = Instant.now();
        for (Payment p : payments.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, now.minus(Duration.ofMinutes(policy.getPendingCheckMinutes())))) {
            if (!"MPESA_STK".equals(p.getProvider()) || p.getCheckoutRequestId() == null) continue;
            if (p.getCreatedAt().isBefore(now.minus(Duration.ofHours(24)))) continue;    // older ones are flagged by reconciliation
            MpesaService.StkQueryResult q = mpesa.stkQuery(p.getCheckoutRequestId());
            if (!q.answered()) continue;
            Long id = p.getId();
            tx.executeWithoutResult(s -> payments.lockById(id).ifPresent(locked -> {
                if (locked.getStatus() != PaymentStatus.PENDING) return;
                if (q.resultCode() == 0) {
                    locked.setStatus(PaymentStatus.NEEDS_REVIEW);
                    addNote(locked, "Safaricom says this payment succeeded but no callback arrived. Find the receipt in the M-Pesa statement and confirm it.");
                } else {
                    locked.setStatus(PaymentStatus.FAILED);
                    addNote(locked, q.description());
                    markOrderFailed(locked.getOrder());
                }
                payments.save(locked);
            }));
        }
    }

    // ------------------------------------------------------------------------------------------ admin

    /** An admin supplies the receipt (read from the M-Pesa statement) for a payment whose callback never arrived. */
    @Transactional
    public Payment adminConfirm(Long paymentId, String receipt, long amountKes, Long adminId) {
        Payment p = payments.lockById(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (p.getStatus() != PaymentStatus.PENDING && p.getStatus() != PaymentStatus.NEEDS_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment is already " + p.getStatus());
        if (p.getMpesaReceiptNumber() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment already has a receipt. Use refund instead");
        if (receipt == null || receipt.isBlank() || amountKes <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt and amount are required");
        addNote(p, "Confirmed by admin #" + adminId);
        complete(p, Math.multiplyExact(amountKes, 100L), receipt.trim().toUpperCase(), null);
        return p;
    }

    // ------------------------------------------------------------------------------------------ helpers

    static long toCents(BigDecimal kes) {
        if (kes == null) return 0;
        return kes.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    static void addNote(Payment p, String text) {
        String n = p.getNote() == null || p.getNote().isBlank() ? text : p.getNote() + " | " + text;
        p.setNote(n.length() > 500 ? n.substring(n.length() - 500) : n);
    }
}
