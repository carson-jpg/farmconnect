package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A price posted by a county officer for one crop at one market on one day. History is kept so trends can be shown. */
@Entity
@Table(name = "market_prices")
@Getter @Setter @NoArgsConstructor
public class MarketPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String crop;
    @Column(nullable = false)
    private String market;
    /** e.g. "90kg bag", "kg", "litre", "tray" */
    @Column(nullable = false)
    private String unit;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User postedBy;
    private Instant createdAt = Instant.now();
}
