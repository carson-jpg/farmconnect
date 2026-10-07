package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Information published by the county: tips, articles, advisories, announcements, programmes, trainings, opportunities. */
@Entity
@Table(name = "posts")
@Getter @Setter @NoArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PostType type;
    @Column(nullable = false, length = 200)
    private String title;
    /** CROPS, LIVESTOCK, PESTS_DISEASES, SOIL_WATER, POST_HARVEST, MARKETS, FINANCE, GENERAL */
    private String topic;
    @Column(length = 6000, nullable = false)
    private String body;
    private String location;
    /** Date of the training / programme / deadline of the opportunity. */
    private Instant eventDate;
    private String contact;
    /** ALL, FARMERS or BUYERS */
    private String audience = "ALL";
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;
    private Instant createdAt = Instant.now();
    private long views;
}
