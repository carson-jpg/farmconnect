package com.farmconnect.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** An approved payout destination. Only VERIFIED + active accounts are ever paid. */
@Entity
@Table(name = "beneficiary_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_benef", columnNames = {"beneficiary_type", "beneficiary_id"}))
@Getter @Setter @NoArgsConstructor
public class BeneficiaryAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "beneficiary_type", nullable = false)
    private BeneficiaryType beneficiaryType;
    @Column(name = "beneficiary_id", nullable = false)
    private String beneficiaryId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AccountType accountType;
    /** Phone, paybill/till number or bank reference. */
    private String accountReference;
    /** PAYBILL only: the account number to credit at the paybill. */
    private String accountNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AccountStatus verificationStatus = AccountStatus.PENDING;
    private Instant verifiedAt;
    private boolean active = true;
    private Instant updatedAt = Instant.now();
}
