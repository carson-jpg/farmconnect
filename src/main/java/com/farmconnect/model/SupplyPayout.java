package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Sending the company its share of one collected payment. */
@Entity
@Table(name = "supply_payouts", indexes = {
        @Index(name = "ix_spo_status", columnList = "status, next_attempt_at"),
        @Index(name = "ix_spo_company", columnList = "company_id")})
@Getter @Setter @NoArgsConstructor
public class SupplyPayout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id")
    private SupplyOrder order;
    @ManyToOne(optional = false) @JoinColumn(name = "payment_id")
    private SupplyPayment payment;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    private long amountCents;
    @Enumerated(EnumType.STRING)
    private PayoutAccountType accountType;
    /** Copied when the payout is sent, so history survives later edits of the company's account. */
    private String destinationAccount;
    private String destinationReference;
    @Column(unique = true)
    private String originatorConversationId;
    private String providerConversationId;
    private String providerTransactionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SupplyPayoutStatus status = SupplyPayoutStatus.QUEUED;
    private int attemptCount;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt = Instant.now();
    private Instant lastAttemptAt;
    @Column(length = 500)
    private String failureReason;
    private Instant createdAt = Instant.now();
    private Instant completedAt;
}
