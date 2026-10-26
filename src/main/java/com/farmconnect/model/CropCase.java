package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A sick-plant case a farmer sent to the county officer: what they saw, up to 3 photos, the guide's guesses and the officer's answer. */
@Entity
@Table(name = "crop_cases")
@Getter @Setter @NoArgsConstructor
public class CropCase {
    public static final String OPEN = "OPEN", ANSWERED = "ANSWERED", CLOSED = "CLOSED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User farmer;
    private String crop;
    /** Symptom keys, comma separated. */
    @Column(length = 600)
    private String symptoms;
    @Column(length = 1000)
    private String description;
    private String subCounty;
    private String county;
    /** What the built-in guide thought most likely (problem key, score) when the case was sent. */
    private String guideProblemKey;
    private Integer guideScore;
    @Column(length = 20, nullable = false)
    private String status = OPEN;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "crop_case_photos", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "path", length = 200)
    private List<String> photos = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewer;
    @Column(length = 1500)
    private String reviewerNote;
    /** The problem key the officer confirmed (a guide key), or empty. */
    private String confirmedProblem;
    private Instant reviewedAt;
    private Instant createdAt = Instant.now();
}
