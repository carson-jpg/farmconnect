package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

/** A user who wants a notification whenever a new price for this crop is posted. */
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
}
