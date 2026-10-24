package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A person who works for the company (owner, manager, driver...). A contact record: it does not log in. */
@Entity
@Table(name = "company_users", indexes = @Index(name = "ix_cm_company", columnList = "company_id"))
@Getter @Setter @NoArgsConstructor
public class CompanyMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id")
    private Company company;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 30)
    private String phone;
    @Column(length = 150)
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MemberRole role = MemberRole.OTHER;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
