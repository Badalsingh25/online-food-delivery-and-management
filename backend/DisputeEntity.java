package com.hungerexpress.dispute;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dispute", indexes = {
    @Index(name = "idx_dispute_order", columnList = "order_id"),
    @Index(name = "idx_dispute_user", columnList = "user_id"),
    @Index(name = "idx_dispute_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisputeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "restaurant_id")
    private Long restaurantId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DisputeType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;
    
    @Column(nullable = false, length = 200)
    private String subject;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;
    
    @Column(name = "resolved_by") // Admin user ID
    private Long resolvedBy;
    
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private java.math.BigDecimal refundAmount;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    
    public enum DisputeType {
        ORDER_NOT_RECEIVED,
        WRONG_ITEMS,
        QUALITY_ISSUE,
        COLD_FOOD,
        MISSING_ITEMS,
        OVERCHARGE,
        DELIVERY_DELAY,
        RUDE_BEHAVIOR,
        OTHER
    }
    
    public enum DisputeStatus {
        OPEN,
        IN_REVIEW,
        RESOLVED,
        REJECTED,
        CLOSED
    }
}
