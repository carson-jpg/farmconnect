package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** One pending "forgot password" code per email. Only a BCrypt hash of the 6-digit code is stored. */
@Entity
@Table(name = "password_reset_codes")
@Getter @Setter @NoArgsConstructor
public class PasswordResetCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String codeHash;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    /** Wrong guesses so far; the code is burned after too many. */
    private int attempts;
}
