package com.farmconnect.payment.model;

import com.farmconnect.model.CustomerOrder;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** One money collection from a buyer. Amounts are integer cents (KES x 100). */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "ix_payments_order", columnList = "order_id"),
        @Index(name = "ix_payments_status", columnList = "status")})
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;
    /** MPESA_STK or MPESA_C2B */
    @Column(nullable = false)
    private String provider;
    private String merchantRequestId;
    @Column(unique = true)
    private String checkoutRequestId;
    /** Safaricom receipt (e.g. SJK1ABC234). Unique: the same receipt can never be credited twice. */
    @Column(unique = true)
    private String mpesaReceiptNumber;
    /** What we asked the buyer to pay (whole shillings, in cents). */
    private long expectedCents;
    /** What Safaricom says was paid. 0 until the callback arrives. */
    private long amountCents;
    /** Money received that is NOT allocated to anyone and is owed back to the buyer (overpayment, duplicate payment, refund). */
    private long excessCents;
    /** Money already sent back to the buyer. */
    private long refundedCents;
    private String phoneNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(length = 500)
    private String note;
    private Instant callbackReceivedAt;
    private Instant createdAt = Instant.now();
}
