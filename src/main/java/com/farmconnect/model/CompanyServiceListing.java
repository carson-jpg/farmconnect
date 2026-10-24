package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** A service a verified company offers farmers: transport, machinery hire, soil testing, irrigation, agronomy... */
@Entity
@Table(name = "company_services", indexes = @Index(name = "ix_cs_company", columnList = "company_id"))
@Getter @Setter @NoArgsConstructor
public class CompanyServiceListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ServiceCategory category;
    @Column(length = 1000)
    private String description;
    /** e.g. "per km", "per acre", "per visit", "per day". */
    @Column(length = 40)
    private String priceBasis;
    /** Null means "price on request". */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;
    /** Comma-separated counties served. Empty means wherever the company operates. */
    @Column(length = 1000)
    private String counties;
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
