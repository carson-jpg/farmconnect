package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** The delivery price list, set by the super admin. One row (id 1). */
@Entity
@Table(name = "delivery_settings")
@Getter @Setter @NoArgsConstructor
public class DeliverySettings {
    @Id
    private Long id = 1L;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal baseFee = new BigDecimal("150");
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal perKmRate = new BigDecimal("30");
    /** Roads are longer than a straight line. 1.3 means "add 30%". */
    @Column(nullable = false)
    private double roadFactor = 1.3;
    @Column(nullable = false)
    private double maxKm = 400;
    private Instant updatedAt = Instant.now();
    private String updatedBy;
}
