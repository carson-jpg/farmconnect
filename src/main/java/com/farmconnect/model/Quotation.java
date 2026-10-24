package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A verified farmer's offer against a produce request. */
@Entity
@Table(name = "quotations", indexes = {
        @Index(name = "ix_q_request", columnList = "request_id"),
        @Index(name = "ix_q_farmer", columnList = "farmer_id")})
@Getter @Setter @NoArgsConstructor
public class Quotation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "request_id")
    private ProduceRequest request;
    @ManyToOne(optional = false) @JoinColumn(name = "farmer_id")
    private User farmer;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;
    private LocalDate availableFrom;
    @Column(length = 500)
    private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private QuotationStatus status = QuotationStatus.SUBMITTED;
    @Column(length = 300)
    private String rejectionReason;
    private Instant createdAt = Instant.now();
    private Instant decidedAt;
}
