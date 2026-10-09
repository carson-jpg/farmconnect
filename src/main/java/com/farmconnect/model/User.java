package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    private String phone;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;
    /** True once an admin/officer approved the farmer's verification. Safe to show publicly. */
    @Column(columnDefinition = "boolean default false not null")
    private boolean verified;
    /** False = suspended by an admin: cannot log in or use the API. */
    @Column(columnDefinition = "boolean default true not null")
    private boolean enabled = true;
    private Instant createdAt = Instant.now();
    /** Last time this account used the API (updated at most once an hour). Used for "active users". */
    private Instant lastActiveAt;
    /** Average seller rating (1-5) and number of reviews; kept up to date by ReviewController. */
    @Column(columnDefinition = "double precision default 0 not null")
    private double ratingAvg;
    @Column(columnDefinition = "integer default 0 not null")
    private int ratingCount;
}