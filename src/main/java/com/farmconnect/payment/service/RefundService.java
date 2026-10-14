package com.farmconnect.payment.service;

import com.farmconnect.model.CustomerOrder;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.AllocationRepository;
import com.farmconnect.payment.repository.PaymentRepository;
import com.farmconnect.payment.repository.PayoutRepository;
import com.farmconnect.repository.OrderRepository;
import com.farmconnect.service.MpesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Refunds and reversals. Nothing is ever edited or deleted: allocations are marked REVERSED and compensating
 * ledger lines are added. A refund is refused if any of the money has already been paid out to a farmer or the county,
 * because that needs a person (finance) to recover it.
 */
@Service
@RequiredArgsConstructor
public class RefundService {
    private final PaymentRepository payments;
    private final AllocationRepository allocations;
    private final PayoutRepository payouts;
    private final OrderRepository orders;
    private final LedgerService ledger;
    private final TransactionTemplate tx;

    /** Full refund of a successful payment: reverse all three allocations, then queue the money back to the buyer. */
    public Payout refundPayment(Long paymentId, String reason, String phoneOverride) {
        return tx.execute(s -> {
            Payment p = payments.lockById(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            if (p.getStatus() != PaymentStatus.SUCCESS)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a SUCCESS payment can be refunded in full. For other cases use refund-excess");

            List<PaymentAllocation> all = allocations.findByPaymentId(paymentId);
            // 1) check first, change nothing until we know the whole refund is possible
            for (PaymentAllocation a : all) {
                if (a.getStatus() == AllocationStatus.REVERSED) continue;
                boolean kept = isRetained(a);
                if (!kept && (a.getStatus() == AllocationStatus.SETTLED || a.getSettledCents() > 0))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            a.getBeneficiaryType() + " has already been paid out. Recover that money with finance first");
                for (Payout po : payouts.findByAllocationId(a.getId())) {
                    boolean inFlight = po.getStatus() == PayoutStatus.PROCESSING || po.getStatus() == PayoutStatus.UNKNOWN
                            || (po.getStatus() == PayoutStatus.SUCCESS && !kept);
                    if (inFlight) throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A payout for " + a.getBeneficiaryType() + " is in flight or complete. Resolve it first");
                }
            }
            // 2) reverse
            long reversed = 0;
            String buyer = String.valueOf(p.getOrder().getBuyer().getId());
            for (PaymentAllocation a : all) {
                if (a.getStatus() == AllocationStatus.REVERSED) continue;
                String account = LedgerService.payable(a.getBeneficiaryType(), a.getBeneficiaryId());
                if (isRetained(a)) ledger.move(p.getId(), a.getId(), "REVERSAL", LedgerService.PLATFORM_REVENUE, account, a.getAmountCents(), "undo retention");
                ledger.move(p.getId(), a.getId(), "REVERSAL", account, LedgerService.COLLECTED, a.getAmountCents(), reason);
                for (Payout po : payouts.findByAllocationId(a.getId()))
                    if (po.getStatus() == PayoutStatus.QUEUED) { po.setStatus(PayoutStatus.CANCELLED); payouts.save(po); }
                a.setStatus(AllocationStatus.REVERSED);
                a.setHoldReason(trunc(reason));
                reversed += a.getAmountCents();
            }
            ledger.move(p.getId(), null, "REFUND_DUE", LedgerService.COLLECTED, LedgerService.payable(BeneficiaryType.BUYER, buyer), reversed, reason);
            p.setExcessCents(p.getExcessCents() + reversed);
            p.setStatus(PaymentStatus.REVERSED);
            PaymentService.addNote(p, "Refunded: " + (reason == null ? "" : reason));
            payments.save(p);

            CustomerOrder o = p.getOrder();
            o.setPaymentStatus("REFUNDED");
            o.setUpdatedAt(Instant.now());
            orders.save(o);
            return queueRefund(p, phoneOverride);
        });
    }

    /** Pay back money that was never allocated (underpayment, overpayment, duplicate payment). */
    public Payout refundExcess(Long paymentId, String phoneOverride) {
        return tx.execute(s -> {
            Payment p = payments.lockById(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
            if (p.getExcessCents() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Nothing is owed back on this payment");
            return queueRefund(p, phoneOverride);
        });
    }

    private Payout queueRefund(Payment p, String phoneOverride) {
        long whole = p.getExcessCents() / 100 * 100;                     // M-Pesa sends whole shillings only
        if (whole <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Less than KES 1 is owed back; it cannot be sent by M-Pesa");
        long inFlight = payouts.findByPaymentId(p.getId()).stream()
                .filter(x -> x.getKind() == PayoutKind.REFUND && x.getStatus() != PayoutStatus.FAILED
                        && x.getStatus() != PayoutStatus.CANCELLED && x.getStatus() != PayoutStatus.SUCCESS)
                .mapToLong(Payout::getAmountCents).sum();
        long amount = whole - inFlight;
        if (amount <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund for this payment is already in progress");

        String raw = phoneOverride != null && !phoneOverride.isBlank() ? phoneOverride : p.getPhoneNumber();
        if (raw == null || raw.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No phone number on record for this payment. Supply the buyer's phone");
        Payout r = new Payout();
        r.setKind(PayoutKind.REFUND);
        r.setPayment(p);
        r.setBeneficiaryType(BeneficiaryType.BUYER);
        r.setBeneficiaryId(String.valueOf(p.getOrder().getBuyer().getId()));
        r.setAccountType(AccountType.MPESA_PHONE);
        r.setDestinationAccount(MpesaService.normalizePhone(raw));
        r.setAmountCents(amount);
        r.setStatus(PayoutStatus.QUEUED);
        return payouts.save(r);
    }

    private boolean isRetained(PaymentAllocation a) {
        return a.getBeneficiaryType() == BeneficiaryType.PLATFORM && payouts.findByAllocationId(a.getId()).stream()
                .anyMatch(po -> po.getAccountType() == AccountType.RETAINED && po.getStatus() == PayoutStatus.SUCCESS);
    }

    private static String trunc(String s) { return s == null ? null : s.length() > 290 ? s.substring(0, 290) : s; }
}
