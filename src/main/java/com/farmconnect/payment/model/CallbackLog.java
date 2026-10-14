package com.farmconnect.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Raw copy of every callback Safaricom sends us. Kept for disputes and reconciliation; never edited. */
@Entity
@Table(name = "payment_callback_logs")
@Getter @Setter @NoArgsConstructor
public class CallbackLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** STK, C2B_VALIDATION, C2B_CONFIRMATION, PAYOUT_RESULT, PAYOUT_TIMEOUT */
    private String kind;
    @Lob @Column(columnDefinition = "text")
    private String payload;
    private Instant receivedAt = Instant.now();
}
