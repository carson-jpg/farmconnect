package com.farmconnect.payment.service;

import com.farmconnect.payment.model.Payment;
import com.farmconnect.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Decides whether a payment notification can be trusted enough to allocate money. */
@Service
@RequiredArgsConstructor
public class PaymentVerificationService {
    public enum Kind { OK, OVERPAID, UNDERPAID, NO_RECEIPT, DUPLICATE_RECEIPT }

    public record Verdict(Kind kind, String reason) {
        public boolean canAllocate() { return kind == Kind.OK || kind == Kind.OVERPAID; }
    }

    private final PaymentRepository payments;

    public Verdict verify(Payment p, long paidCents, String receipt, String callbackPhone) {
        if (receipt == null || receipt.isBlank())
            return new Verdict(Kind.NO_RECEIPT, "No M-Pesa receipt number in the notification");
        var other = payments.findByMpesaReceiptNumber(receipt);
        if (other.isPresent() && !other.get().getId().equals(p.getId()))
            return new Verdict(Kind.DUPLICATE_RECEIPT, "Receipt " + receipt + " was already used by payment #" + other.get().getId());
        if (paidCents < p.getExpectedCents())
            return new Verdict(Kind.UNDERPAID, "Paid KES " + paidCents / 100.0 + " but the order total is KES " + p.getExpectedCents() / 100.0);
        String note = paidCents > p.getExpectedCents() ? "Paid more than the order total" : "ok";
        if (callbackPhone != null && p.getPhoneNumber() != null && !callbackPhone.equals(p.getPhoneNumber()))
            note += " (paid from a different phone than the prompt was sent to)";
        return new Verdict(paidCents > p.getExpectedCents() ? Kind.OVERPAID : Kind.OK, note);
    }
}
