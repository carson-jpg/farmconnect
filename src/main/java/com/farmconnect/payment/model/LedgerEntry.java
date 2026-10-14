package com.farmconnect.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

/**
 * Append-only. Every movement is a DEBIT line and a CREDIT line of the same amount, so for any payment
 * debits always equal credits. Mistakes are fixed with compensating entries, never by editing a row.
 *
 * Accounts used:  MPESA_CLEARING (money sitting in our till), PAYABLE:FARMER:{id}, PAYABLE:COUNTY:{name},
 * PAYABLE:PLATFORM, PAYABLE:BUYER:{id} (refunds / excess owed back), PLATFORM_REVENUE (retained share).
 */
@Entity
@Immutable
@Table(name = "ledger_entries", indexes = {
        @Index(name = "ix_ledger_payment", columnList = "payment_id"),
        @Index(name = "ix_ledger_account", columnList = "account_type")})
@Getter @Setter @NoArgsConstructor
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "allocation_id")
    private Long allocationId;
    /** COLLECTION, ALLOCATION, SETTLEMENT, RETENTION, REVERSAL, REFUND, EXCESS */
    @Column(nullable = false)
    private String entryType;
    @Column(name = "account_type", nullable = false)
    private String accountType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EntryDirection direction;
    private long amountCents;
    private String currency = "KES";
    @Column(length = 200)
    private String reference;
    private Instant createdAt = Instant.now();
}
