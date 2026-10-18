package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A user who wants a notification for this crop. With no target they hear about every new price;
 * with a target (and a direction) they only hear when a market's price first reaches it.
 */
@Entity
@Table(name = "price_alerts", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "crop"}))
@Getter @Setter @NoArgsConstructor
public class PriceAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    @Column(nullable = false)
    private String crop;
    /** Optional price line in KES; null means "alert me on every new price". */
    @Column(precision = 12, scale = 2)
    private BigDecimal targetPrice;
    /** "ABOVE" (price rises to or above the target) or "BELOW"; null when there is no target. */
    @Column(length = 5)
    private String direction;
}
