package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A phone that can receive push notifications (a Firebase Cloud Messaging registration token). */
@Entity
@Table(name = "device_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
@Getter @Setter @NoArgsConstructor
public class DeviceToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    @Column(nullable = false, length = 512)
    private String token;
    private String platform;
    private Instant updatedAt = Instant.now();
}
