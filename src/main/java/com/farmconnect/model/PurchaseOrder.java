package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Agreed terms between a company and a farmer, created when the company accepts a quotation. Prices are copied here. */
@Entity
@Table(name = "bulk_purchase_orders", indexes = {
        @Index(name = "ix_bpo_company", columnList = "company_id"),
        @Index(name = "ix_bpo_farmer", columnList = "farmer_id")})
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "request_id")
    private ProduceRequest request;
    @OneToOne(optional = false) @JoinColumn(name = "quotation_id", unique = true)
    private Quotation quotation;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    @ManyToOne(optional = false) @JoinColumn(name = "farmer_id")
    private User farmer;
    private String produce;
    @Column(precision = 14, scale = 2)
    private BigDecimal quantity;
    private String unit;
    @Column(precision = 12, scale = 2)
    private BigDecimal pricePerUnit;
    @Column(precision = 14, scale = 2)
    private BigDecimal total;
    private String deliveryCounty;
    @Column(length = 250)
    private String deliveryLocation;
    private LocalDate neededBy;

    /** The BULK_PURCHASE rate in force when the order was issued. Recorded here; money is settled between the two parties for now. */
    private Long commissionPolicyId;
    private int platformBps;
    private int countyBps;
    @Column(precision = 14, scale = 2)
    private BigDecimal platformFee;
    @Column(precision = 14, scale = 2)
    private BigDecimal countyFee;
    @Column(precision = 14, scale = 2)
    private BigDecimal farmerNet;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PurchaseOrderStatus status = PurchaseOrderStatus.ISSUED;
    /** UNPAID or PAID. The company marks it PAID after paying the farmer. */
    private String paymentStatus = "UNPAID";
    private Instant paidAt;
    @Column(length = 500)
    private String cancelReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant confirmedAt;
    private Instant deliveredAt;
}
