package com.hungerexpress.orders;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = true)  // Allow guest orders
    private Long userId;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "assigned_to")
    private Long assignedTo; // agent user id

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Address snapshot
    @Column(name = "ship_name", length = 120)
    private String shipName;

    @Column(name = "ship_phone", length = 32)
    private String shipPhone;

    @Column(name = "ship_line1", length = 200)
    private String shipLine1;

    @Column(name = "ship_line2", length = 200)
    private String shipLine2;

    @Column(name = "ship_city", length = 80)
    private String shipCity;

    @Column(name = "ship_state", length = 80)
    private String shipState;

    @Column(name = "ship_postal", length = 20)
    private String shipPostal;

    @Column(name = "ship_country", length = 80)
    private String shipCountry;

    // Status timestamps
    private Instant placedAt;
    private Instant preparingAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (placedAt == null && status == OrderStatus.PLACED) {
            placedAt = Instant.now();
        }
    }
}

