package com.farmconnect.model;

import com.farmconnect.payment.model.EntryDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

/** Append-only, like the produce ledger, but separate from it: every movement is a DEBIT and a CREDIT of the same amount. */
@Entity
@Immutable
@Table(name = "supply_ledger_entries", indexes = @Index(name = "ix_sle_payment", columnList = "supply_payment_id"))
@Getter @Setter @NoArgsConstructor
public class SupplyLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "supply_payment_id")
    private Long supplyPaymentId;
    /** COLLECTION, ALLOCATION, EXCESS, SETTLEMENT */
    @Column(nullable = false)
    private String entryType;
    @Column(name = "account_type", nullable = false)
    private String accountType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EntryDirection direction;
    private long amountCents;
    @Column(length = 200)
    private String reference;
    private Instant createdAt = Instant.now();
}
