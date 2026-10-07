package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "feedback")
@Getter @Setter @NoArgsConstructor
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    private int rating;
    @Column(length = 500)
    private String comment;
    private Instant createdAt = Instant.now();
}
