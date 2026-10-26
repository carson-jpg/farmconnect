package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A cash payment for (a farmer's part of) an order, recorded in the app. The farmer records it, the buyer confirms it and
 * the super admin approves it. Only an approved cash sale counts as paid and creates the commission bill.
 */
@Entity
@Table(name = "cash_sales")
@Getter @Setter @NoArgsConstructor
public class CashSale {
    public static final String RECORDED = "RECORDED", BUYER_CONFIRMED = "BUYER_CONFIRMED", BUYER_DISPUTED = "BUYER_DISPUTED",
            APPROVED = "APPROVED", REJECTED = "REJECTED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User farmer;
    /** The farmer's produce on this order, in cents. Fixed by the server, never typed in. */
    private long amountCents;
    @Column(length = 20, nullable = false)
    private String status = RECORDED;
    @Column(length = 300)
    private String farmerNote;
    @Column(length = 300)
    private String buyerNote;
    /** Why the super admin rejected it, or a note on approval. */
    @Column(length = 300)
    private String decisionNote;
    private Long decidedBy;
    private Instant recordedAt = Instant.now();
    private Instant buyerRespondedAt;
    private Instant decidedAt;
}
