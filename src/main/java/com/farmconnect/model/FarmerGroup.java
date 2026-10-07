package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "farmer_groups")
@Getter @Setter @NoArgsConstructor
public class FarmerGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private GroupType type;
    @Column(length = 1000)
    private String description;
    private String subCounty;
    private String location;
    private String contactPhone;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User createdBy;
    private Instant createdAt = Instant.now();
}
