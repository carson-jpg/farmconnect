package com.farmconnect.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** The part of one payment that belongs to one beneficiary. An entitlement, not proof the money has moved. */
@Entity
@Table(name = "payment_allocations",
        uniqueConstraints = @UniqueConstraint(name = "uq_alloc", columnNames = {"payment_id", "beneficiary_type", "beneficiary_id"}),
        indexes = @Index(name = "ix_alloc_status", columnList = "status"))
@Getter @Setter @NoArgsConstructor
public class PaymentAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    @Enumerated(EnumType.STRING) @Column(name = "beneficiary_type", nullable = false)
    private BeneficiaryType beneficiaryType;
    /** FARMER: the user id. COUNTY: the county name. PLATFORM: "FARMCONNECT". */
    @Column(name = "beneficiary_id", nullable = false)
    private String beneficiaryId;
    /** Basis points of the sale (500 = 5%). For farmers this is their effective share, informational only. */
    private int basisPoints;
    private long amountCents;
    private long settledCents;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AllocationStatus status = AllocationStatus.RECORDED;
    @Column(length = 300)
    private String holdReason;
    private Instant createdAt = Instant.now();
    private Instant settledAt;
}
