package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "app_notifications")
@Getter @Setter @NoArgsConstructor
public class AppNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    /** ORDER, VERIFICATION, MESSAGE, POST, GROUP, SYSTEM */
    private String type;
    private String title;
    @Column(length = 500)
    private String body;
    private Long refId;
    private Instant createdAt = Instant.now();
    private Instant readAt;
}
