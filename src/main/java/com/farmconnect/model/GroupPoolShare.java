package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** How much one member puts into a group deal (produce to bring, or inputs to buy). */
@Entity
@Table(name = "group_pool_shares", uniqueConstraints = @UniqueConstraint(columnNames = {"pool_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class GroupPoolShare {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private GroupPool pool;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal qty;
    private Instant updatedAt = Instant.now();
}
