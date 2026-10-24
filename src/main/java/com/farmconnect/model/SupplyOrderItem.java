package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "supply_order_items")
@Getter @Setter @NoArgsConstructor
public class SupplyOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id")
    private SupplyOrder order;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private CompanyProduct product;
    /** Copied at order time so later edits to the listing do not rewrite history. */
    private String productName;
    private String unit;
    private int quantity;
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;
}
