package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A group deal: members pool produce to sell in bulk (SELL) or pool orders for seed, fertilizer or tools (BUY).
 * Record only: FarmConnect tracks quantities, prices and each member's share, and never handles the money.
 */
@Entity
@Table(name = "group_pools")
@Getter @Setter @NoArgsConstructor
public class GroupPool {
    public static final String SELL = "SELL", BUY = "BUY";
    public static final String OPEN = "OPEN", CLOSED = "CLOSED", DONE = "DONE", CANCELLED = "CANCELLED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FarmerGroup group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User createdBy;
    @Column(length = 10, nullable = false)
    private String kind;
    /** The produce (SELL) or the input (BUY), e.g. "Maize" or "DAP fertilizer". */
    @Column(length = 80, nullable = false)
    private String item;
    @Column(length = 40, nullable = false)
    private String unit;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal targetQty;
    /** SELL: the asking price per unit. BUY: the group price per unit agreed or quoted by the dealer. */
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;
    /** BUY only: the normal single-buyer price, so the saving can be shown. */
    @Column(precision = 12, scale = 2)
    private BigDecimal listPrice;
    private LocalDate closesAt;
    @Column(length = 1000)
    private String notes;
    @Column(length = 20, nullable = false)
    private String status = OPEN;
    @Column(length = 500)
    private String finalNote;
    private Instant createdAt = Instant.now();
    private Instant closedAt;
}
