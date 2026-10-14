package com.farmconnect.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** One actual attempt to move money to a beneficiary (or back to a buyer for a refund). */
@Entity
@Table(name = "payouts", indexes = {
        @Index(name = "ix_payout_status", columnList = "status, next_attempt_at"),
        @Index(name = "ix_payout_alloc", columnList = "allocation_id")})
@Getter @Setter @NoArgsConstructor
public class Payout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PayoutKind kind = PayoutKind.ALLOCATION;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private PaymentAllocation allocation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BeneficiaryType beneficiaryType;
    @Column(nullable = false)
    private String beneficiaryId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AccountType accountType;
    /** Phone (2547XXXXXXXX), paybill/till number, or bank reference. Copied here so history survives account edits. */
    private String destinationAccount;
    /** Extra reference for PAYBILL (the account number to credit). */
    private String destinationReference;
    private long amountCents;
    /** Our own id for this attempt; Safaricom echoes it back and rejects duplicates of it. */
    @Column(unique = true)
    private String originatorConversationId;
    private String providerConversationId;
    private String providerTransactionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PayoutStatus status = PayoutStatus.QUEUED;
    private int attemptCount;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt = Instant.now();
    private Instant lastAttemptAt;
    @Column(length = 500)
    private String failureReason;
    private Instant createdAt = Instant.now();
    private Instant completedAt;
}
