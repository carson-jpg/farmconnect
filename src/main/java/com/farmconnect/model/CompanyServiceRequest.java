package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A farmer asking a company for one of its services. */
@Entity
@Table(name = "company_service_requests", indexes = {
        @Index(name = "ix_csr_company", columnList = "company_id"),
        @Index(name = "ix_csr_farmer", columnList = "farmer_id")})
@Getter @Setter @NoArgsConstructor
public class CompanyServiceRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "service_id")
    private CompanyServiceListing service;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    @ManyToOne(optional = false) @JoinColumn(name = "farmer_id")
    private User farmer;
    @Column(length = 1000)
    private String details;
    private LocalDate preferredDate;
    @Column(length = 250)
    private String location;
    private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ServiceRequestStatus status = ServiceRequestStatus.REQUESTED;
    /** The price the company agreed to when it accepted. */
    @Column(precision = 12, scale = 2)
    private BigDecimal quotedPrice;
    @Column(length = 500)
    private String companyNote;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
