package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A farmer's digital farm record: planting, harvest, livestock, expense, income or note. */
@Entity
@Table(name = "farm_records")
@Getter @Setter @NoArgsConstructor
public class FarmRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Farm farm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RecordType type;
    /** Crop, animal or expense/income item, e.g. "Maize", "Dairy cows", "Fertiliser". */
    @Column(nullable = false)
    private String title;
    @Column(name = "record_date")
    private LocalDate recordDate;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal amount;
    @Column(length = 500)
    private String notes;
    private Instant createdAt = Instant.now();
}
