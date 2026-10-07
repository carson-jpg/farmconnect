package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A user who registered interest in a training / programme / opportunity. */
@Entity
@Table(name = "post_registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class PostRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    private Instant createdAt = Instant.now();
}
