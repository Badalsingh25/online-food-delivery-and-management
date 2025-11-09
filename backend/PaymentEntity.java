package com.hungerexpress.payments;

import com.hungerexpress.orders.OrderEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(nullable = false, length = 32)
    private String provider; // RAZORPAY

    @Column(name = "provider_order_id", length = 80)
    private String providerOrderId;

    @Column(name = "provider_payment_id", length = 80)
    private String providerPaymentId;

    @Column(nullable = false, length = 32)
    private String status; // CREATED, AUTHORIZED, CAPTURED, FAILED

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
