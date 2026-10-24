package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A verified company's bulk buying need: "we want 20 tonnes of maize delivered to Eldoret by 30 November". */
@Entity
@Table(name = "produce_requests", indexes = {
        @Index(name = "ix_pr_company", columnList = "company_id"),
        @Index(name = "ix_pr_status", columnList = "status")})
@Getter @Setter @NoArgsConstructor
public class ProduceRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    @Column(nullable = false, length = 80)
    private String produce;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;
    @Column(nullable = false, length = 20)
    private String unit;
    /** The most the company will pay per unit. Optional. */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxPrice;
    private String deliveryCounty;
    @Column(length = 250)
    private String deliveryLocation;
    private LocalDate neededBy;
    /** Last day farmers can send a quotation. */
    private LocalDate closesAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ProduceRequestStatus status = ProduceRequestStatus.OPEN;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
