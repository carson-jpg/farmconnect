package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** A transporter's job to move one order from the farm to the buyer. FarmConnect records the fee; it does not collect it. */
@Entity
@Table(name = "delivery_jobs", indexes = {@Index(name = "ix_dj_order", columnList = "order_id"), @Index(name = "ix_dj_transporter", columnList = "transporter_id")})
@Getter @Setter @NoArgsConstructor
public class DeliveryJob {
    public static final String REQUESTED = "REQUESTED", ACCEPTED = "ACCEPTED", PICKED_UP = "PICKED_UP",
            DELIVERED = "DELIVERED", DECLINED = "DECLINED", CANCELLED = "CANCELLED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User buyer;
    /** The seller whose produce is collected (the first farmer on the order). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User farmer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id")
    private Company transporter;
    @Column(length = 250)
    private String pickupPlace;
    private Double pickupLat;
    private Double pickupLng;
    @Column(length = 250)
    private String dropAddress;
    private Double dropLat;
    private Double dropLng;
    /** Null until it is worked out from locations, or until the transporter confirms it. */
    private Double distanceKm;
    @Column(precision = 12, scale = 2)
    private BigDecimal fee;
    /** The rates the fee was worked out with, kept so later price changes never change this job. */
    @Column(precision = 12, scale = 2)
    private BigDecimal baseFee;
    @Column(precision = 12, scale = 2)
    private BigDecimal perKmRate;
    @Column(length = 20, nullable = false)
    private String status = REQUESTED;
    @Column(length = 500)
    private String note;
    @Column(length = 300)
    private String declineReason;
    /** The transporter confirms they were paid the fee (cash or M-Pesa, outside the app). */
    private boolean feePaid;
    private Instant createdAt = Instant.now();
    private Instant acceptedAt;
    private Instant pickedUpAt;
    private Instant deliveredAt;
    private Instant feePaidAt;
}
