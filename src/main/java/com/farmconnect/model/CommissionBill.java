package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * What a farmer owes FarmConnect and the county after an approved cash sale (the farmer already holds the cash).
 * Paid by M-Pesa in the app, or marked paid by the super admin.
 */
@Entity
@Table(name = "commission_bills")
@Getter @Setter @NoArgsConstructor
public class CommissionBill {
    public static final String UNPAID = "UNPAID", PENDING = "PENDING", PAID = "PAID";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private Long cashSaleId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User farmer;
    private Long orderId;
    private long saleCents;
    private long platformCents;
    private long countyCents;
    private long totalCents;
    @Column(length = 10, nullable = false)
    private String status = UNPAID;
    @Column(unique = true)
    private String checkoutRequestId;
    private String phone;
    @Column(unique = true)
    private String mpesaReceipt;
    /** MPESA or ADMIN */
    @Column(length = 10)
    private String paidVia;
    @Column(length = 300)
    private String note;
    private Instant stkSentAt;
    private Instant createdAt = Instant.now();
    private Instant paidAt;
}
