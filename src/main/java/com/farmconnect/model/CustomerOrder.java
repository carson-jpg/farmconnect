package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User buyer;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal total;
    private String deliveryAddress;
    private String deliveryPhone;
    @Column(length = 500)
    private String deliveryNote;
    /** CASH_ON_DELIVERY or MPESA_ON_DELIVERY. Recorded only - no online payment is processed yet. */
    private String paymentMethod;
    private Instant updatedAt;
    private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
