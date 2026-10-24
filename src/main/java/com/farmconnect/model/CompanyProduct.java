package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** An input a verified supplier sells to farmers: seed, fertiliser, feed, equipment... */
@Entity
@Table(name = "company_products", indexes = @Index(name = "ix_cp_company", columnList = "company_id"))
@Getter @Setter @NoArgsConstructor
public class CompanyProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id")
    private Company company;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SupplyCategory category;
    @Column(length = 1000)
    private String description;
    /** What one unit is, e.g. "50 kg bag", "litre", "each". */
    @Column(nullable = false, length = 40)
    private String unit;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    private int stockQty;
    private int minOrderQty = 1;
    /** Batch, licence or certification reference (KEPHIS, PCPB...). Shown to farmers. */
    @Column(length = 100)
    private String licenceRef;
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
