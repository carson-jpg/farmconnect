package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(length = 1000)
    private String description;
    private String category;
    @Column(nullable = false)
    private BigDecimal price;
    private int quantity;
    private String unit;
    private String imageUrl;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Farm farm;
}
