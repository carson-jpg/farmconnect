package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * What FarmConnect (and, only under a formally approved agreement, the county) takes from one kind of transaction.
 * Policies are never edited: a new rate closes the old row (effectiveTo) and starts a new one, so every order can
 * point at the exact rate it was charged.
 */
@Entity
@Table(name = "commission_policies", indexes = @Index(name = "ix_policy_type", columnList = "type"))
@Getter @Setter @NoArgsConstructor
public class CommissionPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CommissionType type;
    /** Basis points of the sale (500 = 5%). */
    private int platformBps;
    private int countyBps;
    /** Reference of the formal agreement that allows a county share. Required when countyBps > 0. */
    @Column(length = 200)
    private String countyAgreementRef;
    private Instant effectiveFrom = Instant.now();
    private Instant effectiveTo;
    @ManyToOne
    private User createdBy;
    @Column(length = 300)
    private String note;
    private Instant createdAt = Instant.now();
}
