package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    /** Short one-line summary shown on cards. */
    @Column(length = 200)
    private String summary;
    /** Full description (what it is, how it is grown, harvest date, storage, delivery...). */
    @Column(length = 5000)
    private String description;
    private String category;
    @Column(nullable = false)
    private BigDecimal price;
    private int quantity;
    private String unit;
    private String imageUrl;
    /** Uploaded gallery photos (relative paths, first one is the cover). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "path")
    private List<String> images = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Farm farm;
}
