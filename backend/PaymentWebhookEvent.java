package com.hungerexpress.payments;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_event", indexes = {
        @Index(name = "idx_pwe_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_pwe_signature", columnList = "signature")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 100, unique = true)
    private String eventId;

    @Column(name = "signature", length = 200)
    private String signature;

    @Column(name = "payload_sha256", length = 64)
    private String payloadSha256;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();
}


