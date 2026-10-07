package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** County-curated directory of agro-dealers, vets, extension officers, transporters, buyers... */
@Entity
@Table(name = "service_providers")
@Getter @Setter @NoArgsConstructor
public class ServiceProvider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ServiceCategory category;
    @Column(length = 1000)
    private String description;
    @Column(length = 500)
    private String services;
    private String phone;
    private String email;
    private String subCounty;
    private String location;
    private Instant createdAt = Instant.now();
}
