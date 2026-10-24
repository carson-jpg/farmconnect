package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "company_verification_documents")
@Getter @Setter @NoArgsConstructor
public class CompanyDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id")
    private Company company;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CompanyDocType type;
    /** Relative path inside the upload folder (never public). */
    @Column(nullable = false)
    private String path;
    private String fileName;
    private Instant uploadedAt = Instant.now();
}
