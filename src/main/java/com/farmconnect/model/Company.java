package com.farmconnect.model;

import com.farmconnect.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A business account on FarmConnect. One company per COMPANY user. */
@Entity
@Table(name = "companies")
@Getter @Setter @NoArgsConstructor
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "owner_id", unique = true)
    private User owner;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String businessRegNumber;

    /** Encrypted at rest (AES-GCM). Only the company itself and the super admin ever see it. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String kraPin;
    /** HMAC of the KRA PIN, used only to stop one PIN being used on two companies. */
    @Column(length = 64)
    private String kraPinHash;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CompanyCategory category;
    /** A company can buy produce, sell products/services, or both. */
    private boolean canBuy;
    private boolean canSell;

    private String repName;
    private String repPhone;
    private String repEmail;

    @Column(length = 1000)
    private String description;
    @Column(length = 250)
    private String address;
    private String county;
    /** Comma-separated county names. */
    @Column(length = 1000)
    private String operatingCounties;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.PENDING_VERIFICATION;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant submittedAt = Instant.now();
    private Instant reviewedAt;
    @ManyToOne
    private User reviewedBy;
    @Column(length = 500)
    private String rejectionReason;
    @Column(length = 500)
    private String suspensionReason;

    /** Where FarmConnect sends the company's money. Only used once the super admin has approved it. */
    @Enumerated(EnumType.STRING)
    private PayoutAccountType payoutType;
    private String payoutAccount;
    private String payoutReference;
    /** Wrapper type on purpose: the column is added to a table that already has rows. */
    private Boolean payoutVerified;
    private Instant payoutVerifiedAt;

    public boolean isPayoutApproved() { return Boolean.TRUE.equals(payoutVerified) && payoutType != null && payoutAccount != null; }
}
