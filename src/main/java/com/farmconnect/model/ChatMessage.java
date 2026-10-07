package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User sender;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User recipient;
    @Column(length = 1000, nullable = false)
    private String body;
    private Instant createdAt = Instant.now();
    private Instant readAt;
}
