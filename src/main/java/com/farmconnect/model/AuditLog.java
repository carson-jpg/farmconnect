package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One security-relevant event: a sign-in, a blocked attempt, or something a staff member changed. Only the super admin can read these. */
@Entity
@Table(name = "audit_log", indexes = {@Index(name = "idx_audit_at", columnList = "at")})
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant at = Instant.now();
    private Long actorId;
    private String actorEmail;
    private String actorRole;
    /** LOGIN_OK, LOGIN_FAILED, LOGIN_BLOCKED, PASSWORD_RESET, ACCESS_DENIED, ADMIN_ACTION ... */
    @Column(length = 40)
    private String action;
    @Column(length = 300)
    private String target;
    @Column(length = 500)
    private String detail;
    @Column(length = 64)
    private String ip;
    private boolean success = true;
}
