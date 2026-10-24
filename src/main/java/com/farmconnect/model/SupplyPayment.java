package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** One M-Pesa collection from a farmer for one delivered supply order. Amounts are integer cents (KES x 100). */
@Entity
@Table(name = "supply_payments", indexes = {
        @Index(name = "ix_sp_order", columnList = "order_id"),
        @Index(name = "ix_sp_status", columnList = "status")})
@Getter @Setter @NoArgsConstructor
public class SupplyPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id")
    private SupplyOrder order;
    private String merchantRequestId;
    @Column(unique = true)
    private String checkoutRequestId;
    /** Unique: the same receipt can never be credited twice. */
    @Column(unique = true)
    private String mpesaReceiptNumber;
    private long expectedCents;
    private long amountCents;
    /** Received money that is not allocated to anyone and is owed back to the farmer. */
    private long excessCents;
    private String phoneNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SupplyPaymentStatus status = SupplyPaymentStatus.PENDING;
    @Column(length = 500)
    private String note;
    private Instant callbackReceivedAt;
    private Instant createdAt = Instant.now();
}
