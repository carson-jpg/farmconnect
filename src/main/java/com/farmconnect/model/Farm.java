package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farms")
@Getter @Setter @NoArgsConstructor
public class Farm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String location;
    /** Kwanza, Endebess, Saboti, Kiminini or Cherangany - used for county analytics. */
    private String subCounty;
    @Column(length = 1000)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;
}
