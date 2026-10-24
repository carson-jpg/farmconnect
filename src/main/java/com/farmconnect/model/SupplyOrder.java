package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A farmer's order for inputs from ONE company. Money is paid on delivery for now; the commission is recorded here. */
@Entity
@Table(name = "supply_orders", indexes = {
        @Index(name = "ix_so_company", columnList = "company_id"),
        @Index(name = "ix_so_buyer", columnList = "buyer_id")})
@Getter @Setter @NoArgsConstructor
public class SupplyOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "buyer_id")
    private User buyer;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SupplyOrderStatus status = SupplyOrderStatus.PLACED;

    @Column(precision = 14, scale = 2)
    private BigDecimal total;
    /** The policy rate that applied when the order was placed (a later rate change never touches this order). */
    private Long commissionPolicyId;
    private int platformBps;
    private int countyBps;
    @Column(precision = 14, scale = 2)
    private BigDecimal platformFee;
    @Column(precision = 14, scale = 2)
    private BigDecimal countyFee;
    /** What the company keeps: total - platformFee - countyFee. */
    @Column(precision = 14, scale = 2)
    private BigDecimal companyNet;

    private String deliveryAddress;
    private String deliveryPhone;
    @Column(length = 500)
    private String deliveryNote;
    /** UNPAID or PAID. The company marks it PAID when it has received the money. */
    private String paymentStatus = "UNPAID";
    private Instant paidAt;
    /** CASH (company marked it paid) or MPESA (collected by FarmConnect). */
    private String paymentMethod;
    private String mpesaReceipt;
    @Column(length = 500)
    private String rejectionReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant deliveredAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplyOrderItem> items = new ArrayList<>();
}
