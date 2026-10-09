package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A buyer's rating of a seller after a delivered order. One review per order and seller. */
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "seller_id"}))
@Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User seller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User buyer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;
    private int rating;
    @Column(length = 500)
    private String comment;
    @Column(length = 500)
    private String reply;
    private Instant createdAt = Instant.now();
    private Instant repliedAt;
}
