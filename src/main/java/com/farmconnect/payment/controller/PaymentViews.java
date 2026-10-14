package com.farmconnect.payment.controller;

import com.farmconnect.payment.model.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Safe, flat shapes for the API. Entities are never returned directly (they link to users and orders). */
public final class PaymentViews {
    private PaymentViews() {}

    static BigDecimal kes(long cents) { return BigDecimal.valueOf(cents, 2); }

    public record PaymentView(Long id, Long orderId, String provider, String receipt, BigDecimal expectedKes, BigDecimal amountKes,
                              BigDecimal owedToBuyerKes, BigDecimal refundedKes, PaymentStatus status, String phone, String note,
                              Instant createdAt, Instant callbackAt) {}
    public record AllocationView(Long id, Long paymentId, BeneficiaryType beneficiaryType, String beneficiaryId, int basisPoints,
                                 BigDecimal amountKes, BigDecimal settledKes, AllocationStatus status, String holdReason) {}
    public record PayoutView(Long id, PayoutKind kind, Long paymentId, Long allocationId, BeneficiaryType beneficiaryType, String beneficiaryId,
                             AccountType accountType, String destination, BigDecimal amountKes, PayoutStatus status, int attempts,
                             Instant nextAttemptAt, String failureReason, String providerConversationId, String providerTransactionId,
                             Instant createdAt, Instant completedAt) {}
    public record LedgerView(Long id, String entryType, String account, EntryDirection direction, BigDecimal amountKes, String reference, Instant createdAt) {}
    public record AccountView(Long id, BeneficiaryType beneficiaryType, String beneficiaryId, AccountType accountType, String reference,
                              String accountNumber, AccountStatus status, boolean active) {}

    public static PaymentView of(Payment p) {
        return new PaymentView(p.getId(), p.getOrder().getId(), p.getProvider(), p.getMpesaReceiptNumber(), kes(p.getExpectedCents()),
                kes(p.getAmountCents()), kes(p.getExcessCents()), kes(p.getRefundedCents()), p.getStatus(), p.getPhoneNumber(), p.getNote(),
                p.getCreatedAt(), p.getCallbackReceivedAt());
    }
    public static AllocationView of(PaymentAllocation a) {
        return new AllocationView(a.getId(), a.getPayment().getId(), a.getBeneficiaryType(), a.getBeneficiaryId(), a.getBasisPoints(),
                kes(a.getAmountCents()), kes(a.getSettledCents()), a.getStatus(), a.getHoldReason());
    }
    public static PayoutView of(Payout p) {
        return new PayoutView(p.getId(), p.getKind(), p.getPayment().getId(), p.getAllocation() == null ? null : p.getAllocation().getId(),
                p.getBeneficiaryType(), p.getBeneficiaryId(), p.getAccountType(), p.getDestinationAccount(), kes(p.getAmountCents()), p.getStatus(),
                p.getAttemptCount(), p.getNextAttemptAt(), p.getFailureReason(), p.getProviderConversationId(), p.getProviderTransactionId(),
                p.getCreatedAt(), p.getCompletedAt());
    }
    public static LedgerView of(LedgerEntry e) {
        return new LedgerView(e.getId(), e.getEntryType(), e.getAccountType(), e.getDirection(), kes(e.getAmountCents()), e.getReference(), e.getCreatedAt());
    }
    public static AccountView of(BeneficiaryAccount a) {
        return new AccountView(a.getId(), a.getBeneficiaryType(), a.getBeneficiaryId(), a.getAccountType(), a.getAccountReference(),
                a.getAccountNumber(), a.getVerificationStatus(), a.isActive());
    }
}
