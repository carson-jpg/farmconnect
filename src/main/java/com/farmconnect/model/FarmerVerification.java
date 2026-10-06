package com.farmconnect.model;

import com.farmconnect.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "farmer_verifications")
@Getter @Setter @NoArgsConstructor
public class FarmerVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", unique = true)
    private User farmer;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.DRAFT;

    private String fullName;

    /** Encrypted at rest (AES-GCM). Never returned by public endpoints. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String nationalId;
    /** HMAC of the ID number, used only to stop one ID being used on two accounts. */
    @Column(length = 64)
    private String nationalIdHash;

    private String idFrontPath;
    private String idBackPath;
    private String selfiePath;
    private String proofPath;

    private String phone;
    private boolean phoneVerified;
    private String otpHash;
    private String otpPhone;
    private Instant otpExpiresAt;
    private Instant otpSentAt;
    private int otpAttempts;

    private String county = "Trans Nzoia";
    private String subCounty;
    private String ward;
    private String village;
    private Double farmLat;
    private Double farmLng;

    private String farmingType;
    @Column(length = 500)
    private String mainProduce;
    private Double farmSize; // acres

    private boolean termsAccepted;
    private Instant termsAcceptedAt;

    private Instant submittedAt;
    private Instant reviewedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewedBy;
    @Column(length = 500)
    private String rejectionReason;
}